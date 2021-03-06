package chat

import akka.NotUsed
import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl._
import chat.User.OutgoingMessage
import JsonProtocol._
import spray.json._

import scala.io.StdIn

object Server {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    def newUser(chatId: Int, userName: String): Flow[Message, Message, NotUsed] = {

      val chatRoomActorRef = ChatRooms.findOrCreate(chatId)

      // 새로운 webSocket connection이 생길 때마다 user가 새로 접속한 것으로 간주하고, userActor를 새로 만들어준다.
      val userActor = system.actorOf(Props(new User(chatRoomActorRef)))

      val incomingMessages: Sink[Message, NotUsed] =
        Flow[Message].map {
          case TextMessage.Strict(text) => User.IncomingMessage(text, userName)
        } to Sink.actorRef[User.IncomingMessage](
          userActor, // textMessage가 들어오면 IncommingMessage로 변환해서 userActor로 쏴준다.
          PoisonPill // webSocket connection이 끊어지면 stream을 끊는다.?
        )

      val outgoingMessages: Source[Message, NotUsed] =
        Source.actorRef[OutgoingMessage](10, OverflowStrategy.fail)
          .mapMaterializedValue { outActor /* 이 outActor에다가 message를 쏘면 이 source stream으로 message가 들어온다 */ =>
            userActor ! User.Connected(outActor, userName) // connected 상태가 되면, user actor는 chatRoom actor로부터 ChatMessage를 받아서 outActor로 OutgoingMessage를 쏜다
            NotUsed // outActor를 user actor와 연결시켜주고, outActor는 더이상 사용하지 않을 것이므로 materializedValue를 notUsed로 바꿔줘도 무방하다
          } // Source[OutgoingMessage, NotUsed]
          .map { outMsg =>
            // user actor로부터 OutgoingMessage를 받아서 webSocket 응답으로 뱉어줄 textMessage를 만들어준다
            TextMessage(outMsg.toJson.toString())
          } // Source[TextMessage, NotUsed]

      // sink와 source를 하나의 flow로 합쳐준다. 여기서 incoming과 outgoing은 연결되어있지 않음. 즉 2개는 독립적으로 작동하며 incoming이 없어도 outgoing이 발생할 수 있음
      Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
    }

    val route =
      path("chat" / IntNumber) { chatId =>
        parameter(Symbol("name")) { userName =>
          get {
            handleWebSocketMessages(newUser(chatId, userName))
          }
        }
      }

    val binding = Http().newServerAt("127.0.0.1", 8080).bind(route)

    // the rest of the sample code will go here
    println("Started server at 127.0.0.1:8080, press enter to kill server")
    StdIn.readLine()

    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}

package websocket

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString

object WebsocketRoute {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "my-system")

    implicit val executionContext = system.executionContext

    val pingCounter = new AtomicInteger()

    def greeter: Flow[Message, Message, Any] =
      Flow[Message].mapConcat {
        case tm: TextMessage =>
          TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }

    val route =
      path("greeter") {
        handleWebSocketMessages(greeter)
      }

    Http().newServerAt("127.0.0.1", 8080)
      .adaptSettings(_.mapWebsocketSettings(_.withPeriodicKeepAliveData(() => ByteString(s"debug-${pingCounter.incrementAndGet()}"))))
      .bind(route)
  }
}

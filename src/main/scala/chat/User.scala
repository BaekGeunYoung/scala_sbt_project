package chat

import akka.actor.{Actor, ActorRef}

object User {
  case class Connected(outgoing: ActorRef, userName: String)
  case class IncomingMessage(text: String, author: String)
  case class OutgoingMessage(text: String, author: String)
}

class User(chatRoom: ActorRef) extends Actor {
  import User._

  def receive = {
    case Connected(outgoing, userName) =>
      context.become(connected(outgoing, userName))
  }

  def connected(outgoing: ActorRef, userName: String): Receive = {
    chatRoom ! ChatRoom.Join(userName)

    {
      case IncomingMessage(text, author) =>
        chatRoom ! ChatRoom.ChatMessage(text, author)

      case ChatRoom.ChatMessage(text, author) =>
        outgoing ! OutgoingMessage(text, author)
    }
  }

}

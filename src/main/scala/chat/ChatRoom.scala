package chat

import akka.actor._

object ChatRoom {
  case object Join
  case class ChatMessage(message: String)
}

class ChatRoom extends Actor {
  import ChatRoom._
  var users: Set[ActorRef] = Set.empty

  def receive = {
    case Join =>
      users += sender()
      broadCast(ChatMessage(s"${sender()} joined chat room"))
      // we also would like to remove the user when its actor is stopped
      context.watch(sender())

    case Terminated(user) =>
      users -= user
      broadCast(ChatMessage(s"$user left chat room"))

    case msg: ChatMessage => broadCast(msg)
  }

  def broadCast(msg: ChatMessage): Unit = users.foreach(_ ! msg)
}

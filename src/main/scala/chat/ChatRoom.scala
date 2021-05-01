package chat

import akka.actor._

object ChatRoom {
  case class Join(userName: String)
  case class ChatMessage(message: String, author: String)
}

class ChatRoom extends Actor {
  import ChatRoom._
  var users: Map[String, ActorRef] = Map.empty

  def receive = {
    case Join(userName) =>
      users += userName -> sender()
      broadCast(ChatMessage(s"$userName joined chat room", "admin"))
      // we also would like to remove the user when its actor is stopped
      context.watch(sender())

    case Terminated(user) =>
      val terminatedUser = users.filter { mapEntry => mapEntry._2 == user }.head
      users -= terminatedUser._1

      broadCast(ChatMessage(s"${terminatedUser._1} left chat room", "admin"))

    case msg: ChatMessage => broadCast(msg)
  }

  def broadCast(msg: ChatMessage): Unit = users.values.foreach(_ ! msg)
}

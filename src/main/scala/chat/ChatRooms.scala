package chat

import akka.actor.{ActorRef, ActorSystem, Props}

object ChatRooms {
  var chatRooms: Map[Int, ActorRef] = Map.empty[Int, ActorRef]

  def findOrCreate(number: Int)(implicit actorSystem: ActorSystem): ActorRef = chatRooms.getOrElse(number, createNewChatRoom(number))

  private def createNewChatRoom(number: Int)(implicit actorSystem: ActorSystem): ActorRef = {
    val chatroom = actorSystem.actorOf(Props(new ChatRoom))
    chatRooms += number -> chatroom
    chatroom
  }
}

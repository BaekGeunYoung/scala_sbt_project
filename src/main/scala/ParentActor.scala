import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging

class ParentActor extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case "create" =>
      context.actorOf(Props[ChildActor])
      log.info(s"created a new child - children = ${context.children}")
    case "hi" =>
      log.info("Kids, say hi!")
      for (c <- context.children) c ! "hi"
    case "stop" =>
      log.info("parent stopping")
      context.stop(self)
  }
}

class ChildActor extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case "hi" =>
      val parent = context.parent
      log.info(s"my parent $parent made me say hi!")
  }
  override def postStop() {
    log.info("child stopped!")
  }
}

object ActorsHierarchy extends App {
  // 액터 시스템 생성 . 이름은 mysystem
  val ourSystem = ActorSystem("mysystem")

  val parent = ourSystem.actorOf(Props[ParentActor], "parent")
  parent ! "create"
  parent ! "create"
  Thread.sleep(1000)
  parent ! "hi"
  Thread.sleep(1000)
  parent ! "stop"
  Thread.sleep(1000)
  ourSystem.terminate()

}

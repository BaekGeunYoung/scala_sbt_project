import akka.actor.{Actor, ActorSystem, Props}
import akka.event.Logging

class CheckActor extends Actor {
  import akka.actor.{Identify, ActorIdentity}
  val log = Logging(context.system, this)
  def receive = {
    case path: String =>
      log.info(s"checking path =>  $path")
      context.actorSelection(path) ! Identify(path)
    case ActorIdentity(path, Some(ref)) =>
      log.info(s"found actor $ref on $path")
    case ActorIdentity(path, None) =>
      log.info(s"could not find an actor on $path")
  }
}


object ActorsIdentify extends App {
  val ourSystem = ActorSystem("mysystem")

  val checker = ourSystem.actorOf(Props[CheckActor], "checker")
  checker ! "../*"
  Thread.sleep(1000)
  checker ! "../../*"
  Thread.sleep(1000)
  checker ! "/system/*"
  Thread.sleep(1000)
  checker ! "/user/checker2"
  Thread.sleep(1000)
  checker ! "akka://mysystem/system"
  Thread.sleep(1000)
  ourSystem.stop(checker)
  Thread.sleep(1000)
  ourSystem.terminate()
}

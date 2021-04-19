import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorKilledException, ActorRef, ActorSystem, Kill, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging

class Naughty extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case s: String => log.info(s)
    case _ => throw new RuntimeException
  }
  override def postRestart(t: Throwable) = log.info("naughty restarted")
}


class Supervisor extends Actor {
  val child: ActorRef = context.actorOf(Props[Naughty], "victim")
  def receive: Receive = {
    case _ => println(context.children)
  }
  override val supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy() {
      case _: ActorKilledException => Restart
      case _ => Escalate
    }
}

object SupervisionKill extends App {
  val ourSystem = ActorSystem("mysystem")

  val s = ourSystem.actorOf(Props[Supervisor], "super")

  s ! "abc"

  ourSystem.actorSelection("/user/super/*") ! Kill
  ourSystem.actorSelection("/user/super/*") ! "sorry about that"
  ourSystem.actorSelection("/user/super/*") ! "kaboom".toList
  Thread.sleep(1000)
  ourSystem.stop(s)
  Thread.sleep(1000)
  ourSystem.terminate()
}

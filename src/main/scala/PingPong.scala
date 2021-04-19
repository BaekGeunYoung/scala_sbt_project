
import akka.actor._
import akka.event.Logging
import mysystem._


object mysystem2 {
  lazy val ourSystem = ActorSystem("OurExampleSystem")
}


class Pongy2 extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "ping" =>
      log.info("Got a ping -- ponging back!")
      sender ! "pong"
      context.stop(self)
  }
  override def postStop() = log.info("pongy going down")
}


class Pingy2 extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case pongyRef: ActorRef =>
      pongyRef ! "ping"
    case "pong" =>
      log.info("got a pong back!")
      context.stop(self)
  }
  override def postStop() = log.info("ping going down")
}


class Master2 extends Actor {
  val log = Logging(context.system, this)
  val pingy = ourSystem.actorOf(Props[Pingy], "pingy")
  val pongy = ourSystem.actorOf(Props[Pongy], "pongy")

  def receive = {
    case "start" =>
      pingy ! pongy
  }
  override def postStop() = log.info("master going down")
}


object CommunicatingAsk2 extends App {
  val masta = ourSystem.actorOf(Props[Master], "masta")
  masta ! "start"
  Thread.sleep(5000)
  ourSystem.terminate()
}

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import mysystem._


object  mysystem {
  lazy val ourSystem = ActorSystem("OurExampleSystem")
}

class Pongy extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "ping" =>
      log.info("Got a ping -- ponging back!")
      sender ! "pong"
      context.stop(self)
  }
  override def postStop() = log.info("pongy going down")
}


class Pingy extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case pongyRef: ActorRef =>
      implicit val timeout: Timeout = Timeout(FiniteDuration(2, TimeUnit.SECONDS))
      val future = pongyRef ? "ping"
      pipe(future) to sender
  }
  override def postStop() = log.info("Pingy going down")
}


class Master extends Actor {
  val log = Logging(context.system, this)
  val pingy = ourSystem.actorOf(Props[Pingy], "pingy")
  val pongy = ourSystem.actorOf(Props[Pongy], "pongy")
  def receive = {
    case "start" =>
      pingy ! pongy
    case "pong" =>
      log.info("got a pong back!")
      context.stop(self)
  }
  override def postStop() = log.info("master going down")
}


object CommunicatingAsk extends App {
  val masta = ourSystem.actorOf(Props[Master], "masta")
  masta ! "start"
  Thread.sleep(1000)
  ourSystem.terminate()
}

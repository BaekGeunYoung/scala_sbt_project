package remote

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.util.Timeout
import akka.pattern.{ask, pipe}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory

class Pingy extends Actor {
  def receive = {
    case pongyRef: ActorRef =>
      implicit val timeout = Timeout(FiniteDuration(2, TimeUnit.SECONDS))
      val future = pongyRef ? "ping"
      pipe(future) to sender
  }
}

class Runner extends Actor {
  val log = Logging(context.system, this)
  val pingy = context.actorOf(Props[Pingy], "pingy")
  def receive = {
    case "start" =>
      val path = context.actorSelection("akka.tcp://PongyDimension@127.0.0.1:3000/user/pongy") // 보낼 주소
      path ! Identify(0)
    case ActorIdentity(0, Some(ref)) =>
      pingy ! ref
    case ActorIdentity(0, None) =>
      log.info("Something's wrong -- no pongy anywhere!")
      context.stop(self)
    case "pong" =>
      log.info("got a pong from another dimension.")
      context.stop(self)
  }
}


object RemotingPingySystem extends App {

  val config = ConfigFactory.load()

  val system = ActorSystem("PongyDimension", config.getConfig("pingySystem"))

  val runner = system.actorOf(Props[Runner], "runner")
  runner ! "start"
  Thread.sleep(20000)
  system.terminate()
}

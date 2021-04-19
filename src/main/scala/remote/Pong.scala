package remote

import akka.actor._
import akka.event.Logging
import com.typesafe.config.ConfigFactory


class Pongy extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case "ping" =>
      log.info("Got a ping -- ponging back!")
      sender ! "pong"
      context.stop(self)
    case Identify(n) =>
      log.info(s"Got identity - $n")
  }
  override def postStop() = log.info("pongy going down")
}


object RemotingPongySystem extends App {
  val config = ConfigFactory.load()

  val system = ActorSystem("PongyDimension", config.getConfig("pongySystem"))
  val pongy = system.actorOf(Props[Pongy], "pongy")
  Thread.sleep(30000)
  system.terminate()
}


import java.nio.charset.StandardCharsets

import akka.actor._
import akka.event.Logging

import scala.io.Source
import scala.collection._
import mysystem._
import org.apache.commons.io.FileUtils

class DictionaryActor extends Actor {
  private val log = Logging(context.system, this)
  private val dictionary = mutable.Set[String]()

  def receive = uninitialized

  def uninitialized: PartialFunction[Any, Unit] = {
    case DictionaryActor.Init(path) =>
      val fileContent = FileUtils.readFileToString(new java.io.File(path), StandardCharsets.ISO_8859_1)

      fileContent.split("[\\s]+").foreach(s => dictionary += s)

      context.become(initialized)
  }

  def initialized: PartialFunction[Any, Unit] = {
    case DictionaryActor.IsWord(w) =>
      log.info(s"word '$w' exists: ${dictionary(w)}")
    case DictionaryActor.End =>
      dictionary.clear()
      context.become(uninitialized)
  }
  override def unhandled(msg: Any) = {
    log.info(s"message $msg should not be sent in this state.")
  }
}


object DictionaryActor {
  case class Init(path: String)
  case class IsWord(w: String)
  case object End
}


object ActorsBecome extends App {
  val dict = ourSystem.actorOf(Props[DictionaryActor], "dictionary")
  dict ! DictionaryActor.IsWord("program")
  Thread.sleep(1000)
  dict ! DictionaryActor.Init("url-spec.txt")
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("program")
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("balaban")
  Thread.sleep(1000)
  dict ! DictionaryActor.End
  Thread.sleep(1000)
  dict ! DictionaryActor.IsWord("termination")
  Thread.sleep(1000)
  ourSystem.terminate()
}
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.actor.SupervisorStrategy._
import org.apache.commons.io.FileUtils

import scala.io.Source
import scala.collection._
import scala.concurrent.duration._
import mysystem._

class Downloader extends Actor {
  def receive = {
    case DownloadManager.Download(url, dest) =>
      val content = Source.fromURL(url)
      FileUtils.write(new java.io.File(dest), content.mkString)
      sender ! DownloadManager.Finished(dest)
  }
}


class DownloadManager(val downloadSlots: Int) extends Actor {
  val log = Logging(context.system, this)
  val downloaders = mutable.Queue[ActorRef]()
  val pendingWork = mutable.Queue[DownloadManager.Download]()
  val workItems = mutable.Map[ActorRef, DownloadManager.Download]()

  override def preStart(): Unit = {
    for (i <- 0 until downloadSlots) downloaders.enqueue(context.actorOf(Props[Downloader], s"dl$i"))
  }

  private def checkMoreDownloads(): Unit = {
    if (pendingWork.nonEmpty && downloaders.nonEmpty) {
      val dl = downloaders.dequeue()
      val workItem = pendingWork.dequeue()
      log.info(s"$workItem starting, ${downloaders.size} download slots left")
      dl ! workItem
      workItems(dl) = workItem
    }
  }

  def receive = {
    case msg @ DownloadManager.Download(url, dest) =>
      pendingWork.enqueue(msg)
      checkMoreDownloads()
    case DownloadManager.Finished(dest) =>
      workItems.remove(sender)
      downloaders.enqueue(sender)
      log.info(s"Down to '$dest' finished, ${downloaders.size} down slots left")
      checkMoreDownloads()
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 6, withinTimeRange = FiniteDuration(30, TimeUnit.SECONDS)) {
      case fnf: java.io.FileNotFoundException =>
        log.info(s"Resource could not be found: $fnf")
        workItems.remove(sender)
        downloaders.enqueue(sender)
        Resume
      case _ =>
        Escalate
    }
}


object DownloadManager {
  case class Download(url: String, dest: String)
  case class Finished(dest: String)
}


object SupervisionDownloader extends App {
  import DownloadManager._
  val manager = ourSystem.actorOf(Props(classOf[DownloadManager], 4), "manager")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec1.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec2.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec3.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec4.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec5.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec6.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec7.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec8.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec9.txt")
  manager ! Download("http://www.w3.org/Addressing/URL/url-spec.txt", "url-spec10.txt")
  Thread.sleep(5000)
  ourSystem.stop(manager)
  Thread.sleep(5000)
  ourSystem.terminate()
}

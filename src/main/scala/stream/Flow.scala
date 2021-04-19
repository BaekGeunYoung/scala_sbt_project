package stream

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Framing, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString

import scala.concurrent.{Await, Future, duration}

object FlowApp extends App {
  //actorsystem
  implicit val system = ActorSystem("QuickStart")
  //실체화시 암시적인자 필요
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  def parseLine(value: String): MyParseResult = {
    if (value.length > 10) Success(value)
    else Fail(s"string should be longer than 10: $value")
  }

  val source: Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(Paths.get("/Users/geunyoung/scala_sbt_project/src/main/scala/stream/stream-source.txt"))

  //framing 을 이용하여 MyParseResult 단위를 구분한다.
  val frame: Flow[ByteString, String, NotUsed] =
    Framing.delimiter(ByteString("\n"), 1024 * 1024)
      .map(b => b.decodeString("UTF-8"))

  //parser String 을 MyParseResult로 변환 한다.
  //collect method는 return type이 Flow#Repr 으로 type Repr[+T] = Flow[In, T, Mat]
  val parser: Flow[String, MyParseResult, NotUsed] =
  Flow[String].map(s => parseLine(s))

  //Error 상태만 filtering
  val filter: Flow[MyParseResult, MyParseResult, NotUsed] =
    Flow[MyParseResult].filter {
      case _: Fail => true
      case _ => false
    }

  //sink
  val sink: Sink[MyParseResult, Future[Seq[MyParseResult]]] = Sink.seq[MyParseResult]

  val runGraph: RunnableGraph[Future[Seq[MyParseResult]]] =
    source.via(frame).via(parser).via(filter).toMat(sink)(Keep.right)

  val result: Future[Seq[MyParseResult]] = runGraph.run()

  val finalResult = Await.result(result, duration.FiniteDuration(10, TimeUnit.SECONDS))

  finalResult.foreach(println(_))

  system.terminate()
}

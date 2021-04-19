package stream

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, RunnableGraph, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

object SourceToSink extends App {
  //actorsystem
  implicit val system = ActorSystem("QuickStart")
  //실체화시 암시적인자 필요
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  //어디로 보낼지는 모르지만 file 읽어 file내용을 ByteString으로
  val source2: Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(Paths.get("/Users/geunyoung/scala_sbt_project/src/main/scala/stream/factorial.txt"))

  //어떤 거에 연결 될지 모르지만 ByteString을 받아 File 로
  val sink: Sink[ByteString, Future[IOResult]] =
    FileIO.toPath(Paths.get("/Users/geunyoung/scala_sbt_project/src/main/scala/stream/factorial2.txt"))

  //source + sink = 1개의 열린 출력 + 1개의 열린 입력 = Graph(여기서의 최종 설계도)
  val graph: RunnableGraph[Future[IOResult]] = source2 to sink

  //설계도를 실행. 실행결과 로는 Future에 Source에서의 IOResult가 들어있고 이를 println으로 console출력 한다.graph.run은 Future[IOResult] 이다.
  graph.run().foreach { result =>
    println(s"${result.status}, ${result.count} bytes read.")
    system.terminate()
  }

  system.terminate()
}

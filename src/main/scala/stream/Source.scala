package stream

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, RunnableGraph, Sink, Source}
import akka.util.ByteString

import scala.concurrent.{Await, Future, duration}
import scala.util.{Failure, Success}

object SourceApp extends App {
  //actorsystem
  implicit val system = ActorSystem("QuickStart")
  //실체화시 암시적인자 필요
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  //(1 to 100) 까지의 Int를 열린출력으로 내보냄을 나타내는 설계도
  //NotUsed는 실체화 값을 가지지 않는다.
  //이는 설계도 일뿐 실행시 실제로 처리가 진행
  val source: Source[Int, NotUsed] = Source(1 to 100)

  //처리가 진행
  //처리 결과의 실체화는 Future[Done]
  val result: Future[Done] = source.runForeach {i =>
    println(s"$i ${Thread.currentThread().getName}")
  }(materializer)


  println(Thread.currentThread().getName)
  system.terminate()

  result.onComplete{
    case Success(a) => println(a)
    case Failure(e) => println(e)
  }

  //Source는 Graph이며 stream처리 설계도 이고 열린 output 출력을 가진다.
  val factorial:Source[BigInt, NotUsed] =
    source.scan(BigInt(1))((ac, i) => ac * i)

  //materializer를 가지고 실행시 return type는 처리한 byte수의 Future[IOResult] 이다.
//  val fileResult: Future[IOResult] =
//    factorial.map(num => ByteString(s"$num\n"))
//      .runWith(FileIO.toPath(Paths.get("/Users/geunyoung/scala_sbt_project/stream/factorial.txt")))

  val factorialResultFuture: Future[String] = factorial.runFold("")((acc, i) => s"$acc\n$i")

//  val factorialResult = Await.result(factorialResultFuture, duration.FiniteDuration(10, TimeUnit.SECONDS))
//
//  println(factorialResult)

  factorialResultFuture.onComplete {
    case Success(a) => println(a)
    case Failure(e) => println(e)
  }

  system.terminate()
}

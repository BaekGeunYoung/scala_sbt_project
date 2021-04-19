package http

import java.nio.file.Paths

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Keep, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

object HttpServerRoutingMinimal {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "my-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val sink: Sink[ByteString, Future[IOResult]] =
      FileIO.toPath(Paths.get("/Users/geunyoung/scala_sbt_project/src/main/scala/stream/http_result.txt"))

    val source: Source[ByteString, Future[IOResult]] =
      FileIO.fromPath(Paths.get("/Users/geunyoung/scala_sbt_project/src/main/scala/stream/http_result.txt"))

    final case class MyHttpResponse(message: String, byteCount: Long)

    implicit val itemFormat = jsonFormat2(MyHttpResponse)

    val route =
      path("hello") {
        concat(
          get {
            complete(HttpEntity(ContentTypes.`application/json`, source))
          },
          post {
            entity(as[HttpEntity]) { entity =>
              onComplete(
                entity.dataBytes
                  .toMat(sink)(Keep.right)
                  .run()
              ) {
                case Success(IOResult(count, Success(Done))) =>
                  complete(
                    StatusCodes.OK, MyHttpResponse("success", count)
                  )
                case Success(IOResult(_, Failure(e))) =>
                  complete((
                    StatusCodes.BadRequest,
                    e.getMessage
                  ))
                case Failure(e) =>
                  complete((
                    StatusCodes.BadRequest,
                    e.getMessage
                  ))
              }
            }
          }
        )
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}


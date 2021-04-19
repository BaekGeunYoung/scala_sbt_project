package slick

import java.util.concurrent.TimeUnit

import akka.remote.WireFormats.FiniteDuration
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, duration}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

// 모델 정의
class Users(tag: Tag) extends Table[(Int, String, Int, String)](tag, "users") {
  def id = column[Int]("id")
  def username = column[String]("username")
  def age = column[Int]("age")
  def birthDate = column[String]("date")
  def * = (id, username, age, birthDate)
}

object managementMain extends App {

  val connectionUrl = "jdbc:postgresql://127.0.0.1:20200/slick?user=postgres&password=password"
  val db = Database.forURL(connectionUrl, driver = "org.postgresql.Driver")

  try {
    val users = TableQuery[Users]

    val setup = DBIO.seq(
      // Create the tables, including primary and foreign keys
      (users.schema).create,

      // Insert some suppliers
      users += (123, "geunyoung", 24, "1998-07-09"),
      users += (124, "geunyoung", 24, "1998-07-09"),
      users += (125, "geunyoung", 24, "1998-07-09"),
      users += (126, "geunyoung", 24, "1998-07-09"),
    )

    Await.result(db.run(setup), duration.FiniteDuration(10, TimeUnit.SECONDS))
    Await.result(db.stream(users.result).foreach(println), duration.FiniteDuration(10, TimeUnit.SECONDS))

  } finally db.close()
}

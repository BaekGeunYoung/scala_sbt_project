package stream

sealed abstract class MyParseResult

case class Success(val result: String) extends MyParseResult
case class Fail(val message: String) extends MyParseResult


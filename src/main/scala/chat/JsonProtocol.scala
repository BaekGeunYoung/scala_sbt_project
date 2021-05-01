package chat

import chat.User.OutgoingMessage
import spray.json.DefaultJsonProtocol

object JsonProtocol extends DefaultJsonProtocol {
  implicit val messageFormat = jsonFormat2(OutgoingMessage)
}

package monitoring.message

import play.api.libs.json.Json

case class Result(result: String)

object Result {

  implicit val resultFormats = Json.format[Result]

}

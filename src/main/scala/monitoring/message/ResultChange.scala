package monitoring.message

import play.api.libs.json.Json

case class ResultChange(result: Result)

object ResultChange {

  implicit val changeFormats = Json.format[ResultChange]

}

package monitoring.message

import play.api.libs.json.Json

case class ExecuteSubQuery(query: String, endpoint: String)

object ExecuteSubQuery {

  implicit val esqFormats = Json.format[ExecuteSubQuery]

}
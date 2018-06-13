package monitoring.message

import play.api.libs.json.Json

case class FederateQuery(query: String)

object FederateQuery {

  implicit val fqFormats = Json.format[FederateQuery]

}
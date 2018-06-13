package monitoring.message

import play.api.libs.json.Json

case class FederateSubQuery(query: String, endpoints: Seq[String])

object FederateSubQuery {

  implicit val fsqFormats = Json.format[FederateSubQuery]

}
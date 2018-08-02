package monitoring.message

import play.api.libs.json.Json

case class ScheduledQuery(executeSubQuery: ExecuteSubQuery)

object ScheduledQuery {
  implicit val scheduledFormats = Json.format[ScheduledQuery]
}

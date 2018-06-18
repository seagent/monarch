package monitoring.message

import java.io.ByteArrayInputStream

import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory}
import play.api.libs.json.Json

case class Result(resultJSON: String) {
  def toResultSet(): ResultSet = {
    val inputStream = new ByteArrayInputStream(resultJSON.getBytes)
    ResultSetFactory.fromJSON(inputStream)
  }
}

object Result {

  implicit val resultFormats = Json.format[Result]

}

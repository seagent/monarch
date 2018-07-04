package monitoring.message

import java.io.ByteArrayInputStream

import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory}
import play.api.libs.json.Json

//result vars ArrayBuffer[String] olarak buraya da eklenerek yalnızca variable'lara erişmek için yapılan toResultSet durumundan kaçınılmış olacak
case class Result(resultJSON: String, resultVars: Seq[String]) {
  def toResultSet: ResultSet = {
    val inputStream = new ByteArrayInputStream(resultJSON.getBytes)
    ResultSetFactory.fromJSON(inputStream)
  }
}

object Result {

  implicit val resultFormats = Json.format[Result]

}

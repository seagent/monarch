package monitoring.main

import java.io.ByteArrayOutputStream

import com.hp.hpl.jena.query.{ResultSet, ResultSetFormatter}
import monitoring.message.Result
import play.api.Logger
import play.api.libs.json.Json

import scala.collection.JavaConverters._

object MonitoringUtils {
  def convertRdf2Result(rdfResult: ResultSet) = {
    val json = convertRdf2Json(rdfResult)
    Result(json, rdfResult.getResultVars.asScala, 1)
  }

  def convertRdf2Json(rdfResult: ResultSet) = {
    val outputStream = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(outputStream, rdfResult)
    Json.parse(outputStream.toByteArray)
  }

  def printActorCount: Unit = {
    Logger.debug(s"Current actor count is: ${RedisStore.get(Constants.ACTOR_COUNT).get}")
  }

  def printQueryCount: Unit = {
    Logger.debug(s"Current query count is: ${RedisStore.get(Constants.QUERY_COUNT).get}")
  }
}

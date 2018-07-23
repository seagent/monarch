package monitoring.main

import java.io.ByteArrayOutputStream

import com.hp.hpl.jena.query.{QueryExecution, ResultSet, ResultSetFormatter}
import monitoring.message.Result

import scala.collection.JavaConverters._

object MonitoringUtils {
  def convertRdfToResult(rdfResult: ResultSet) = {
    val outputStream = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(outputStream, rdfResult)
    val json = new String(outputStream.toByteArray)
    Result(json, rdfResult.getResultVars.asScala)
  }
}

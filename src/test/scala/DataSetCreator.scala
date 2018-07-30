import java.io.{File, FileWriter, PrintWriter}

import com.hp.hpl.jena.query.{QueryExecutionFactory, ResultSetFactory}
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import monitoring.main.MonitoringUtils
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.json.Reads._

import scala.collection.mutable.ArrayBuffer

object DataSetCreator {

  val DBPEDIA_ENDPOINT = "http://dbpedia.org/sparql"
  val DBPEDIA_DIRECTOR_SELECT_QUERY = "select * where {?movie <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Steven_Spielberg>. ?movie <http://xmlns.com/foaf/0.1/name> ?name}"
  val DBPEDIA_DIRECTOR_CONSTRUCT_QUERY = "construct {?movie <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Steven_Spielberg>. ?movie <http://xmlns.com/foaf/0.1/name> ?name} where {?movie <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Steven_Spielberg>. ?movie <http://xmlns.com/foaf/0.1/name> ?name}"
  val RESULT_FILE_NAME = "src/test/files/query-results.json"
  val ACTUAL_RESULT_FILE_NAME = "src/test/files/actual-results.json"
  val RDF_FILE_NAME = "src/test/files/rdf-model.ttl"

  def main(args: Array[String]): Unit = {

    //createResultSetFile

    //createModelFile

    val res = ResultSetFactory.load(DataSetCreator.ACTUAL_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
    val jsonText = MonitoringUtils.convertRdf2Json(res)
    val jsonValue = Json.parse(jsonText)
    val movieToRemove =
      Json.parse(
        """
    {"movie":{"type":"uri","value":"http://dbpedia.org/resource/Indiana_Jones_and_the_Last_Crusade"},"name":{"type":"literal","xml:lang":"en","value":"Indiana Jones and the Last Crusade"}}
      """)
    val jsObject = jsonValue.as[JsObject]
    println(jsObject)
    val movieArray = (jsObject \ "results" \ "bindings").as[JsArray].value.filterNot(movie => movie == movieToRemove)
    println(movieArray)
    //val transformer = (__ \ "results" \ "bindings").json.update(__.read(jsObject).filterNot(movie => movie == movieToRemove.as[JsObject]))


    val jsonTransformer = (__ \ "results" \ "bindings").json.update(
      of[JsArray].map{ case JsArray(arr) => JsArray(arr.filterNot(movie => movie == Json.obj("movie" -> Json.obj("type" -> "uri", "value" -> "http://dbpedia.org/resource/Indiana_Jones_and_the_Last_Crusade"), "name" -> Json.obj("type" -> "literal", "xml:lang" -> "en", "value" -> "Indiana Jones and the Last Crusade")))) }
    )
    //filterNot(movie => movie == Json.obj("movie" -> Json.obj("type" -> "uri", "value" -> "http://dbpedia.org/resource/Indiana_Jones_and_the_Last_Crusade"), "name" -> Json.obj("type" -> "literal", "xml:lang" -> "en", "value" -> "Indiana Jones and the Last Crusade")))

    val transformer = (__ \ "results" \ "bindings").json.put(JsArray.apply(movieArray))
    val jsResult = jsObject.transform(jsonTransformer).asOpt.getOrElse()

    println(jsResult)

    /*val exampleObject = Json.parse("""{"movie":"jurassic park","director":"steven spielberg","country":{"name":"usa","population":"250M"}}""")
    val transformer = (__).json.update(
      __.read[JsObject].map(movie => movie ++ Json.obj("music" -> "thriller"))
    )
    val exampleResult = exampleObject.transform(transformer)
    println(exampleResult)*/
  }

  private def createModelFile = {
    val qexecConstruct = QueryExecutionFactory.sparqlService(DBPEDIA_ENDPOINT, DBPEDIA_DIRECTOR_CONSTRUCT_QUERY)
    val model = qexecConstruct.execConstruct
    model.write(new FileWriter(new File(RDF_FILE_NAME)), "TTL")
    qexecConstruct.close()
  }

  private def createResultSetFile = {
    //execute query and transform result to json
    val qexecSelect = QueryExecutionFactory.sparqlService(DBPEDIA_ENDPOINT, DBPEDIA_DIRECTOR_SELECT_QUERY)
    val rs = qexecSelect.execSelect
    val jsonRes = MonitoringUtils.convertRdf2Json(rs)
    qexecSelect.close()

    //write json text to file
    val pw = new PrintWriter(new File(RESULT_FILE_NAME))
    pw.write(jsonRes)
    pw.close()
  }
}

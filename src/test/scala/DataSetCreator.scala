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
  val PERSON_SELECT_QUERY = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> select * where {?s rdf:type foaf:Person} OFFSET 10 LIMIT 10"
  val RESULT_FILE_NAME = "src/test/files/query-results.json"
  val ACTUAL_RESULT_FILE_NAME = "src/test/files/actual-results.json"
  val DBPEDIA_RESULT_FILE_NAME = "src/test/files/person-dbpedia.json"
  val IMDB_RESULT_FILE_NAME = "src/test/files/person-imdb.json"
  val RDF_FILE_NAME = "src/test/files/rdf-model.ttl"

  def main(args: Array[String]): Unit = {

    createResultSetFile

    createModelFile

    //val jsResult= changeResultFile(DataSetCreator.ACTUAL_RESULT_FILE_NAME)
    //println(jsResult)
  }

  def changeResultFile(filePathToChange: String): String = {
    val res = ResultSetFactory.load(filePathToChange, ResultsFormat.FMT_RS_JSON)
    val jsonText = MonitoringUtils.convertRdf2Json(res)
    val jsonValue = Json.parse(jsonText)
    val movieToRemove =
      Json.parse(
        """
          |{
          |"movie":{"type":"uri","value":"http://dbpedia.org/resource/Indiana_Jones_and_the_Last_Crusade"},
          |"name":{"type":"literal","xml:lang":"en","value":"Indiana Jones and the Last Crusade"}
          |}
        """.stripMargin)
    val jsObject = jsonValue.as[JsObject]

    val jsonTransformer = (__ \ "results" \ "bindings").json.update(
      of[JsArray].map { case JsArray(arr) => JsArray(arr.filterNot(movie => movie == movieToRemove)) }
    )

    jsObject.transform(jsonTransformer).asOpt.getOrElse().toString
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

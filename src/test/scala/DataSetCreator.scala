import java.io.{File, FileWriter, PrintWriter}

import com.hp.hpl.jena.query.QueryExecutionFactory
import monitoring.main.MonitoringUtils

object DataSetCreator {

  val DBPEDIA_ENDPOINT = "http://dbpedia.org/sparql"
  val DBPEDIA_DIRECTOR_SELECT_QUERY = "select * where {?movie <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Steven_Spielberg>. ?movie <http://xmlns.com/foaf/0.1/name> ?name}"
  val DBPEDIA_DIRECTOR_CONSTRUCT_QUERY = "construct {?movie <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Steven_Spielberg>. ?movie <http://xmlns.com/foaf/0.1/name> ?name} where {?movie <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Steven_Spielberg>. ?movie <http://xmlns.com/foaf/0.1/name> ?name}"
  val RESULT_FILE_NAME = "src/test/files/query-results.json"
  val RDF_FILE_NAME = "src/test/files/rdf-model.ttl"

  def main(args: Array[String]): Unit = {

    createResultSetFile

    createModelFile

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

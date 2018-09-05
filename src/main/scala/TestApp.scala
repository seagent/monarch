import java.io.{File, FileOutputStream}

import com.hp.hpl.jena.query.{QueryExecutionFactory, ResultSetFormatter}

object TestApp {
  val GEO_JOIN_RESULT_NAME = "src/test/files/geo-join.json"
  def main(args: Array[String]): Unit = {
    val rs = QueryExecutionFactory.sparqlService("http://linkedgeodata.org/sparql", "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX geo: <http://www.geonames.org/ontology#> Select * {?geoPlace rdfs:seeAlso <http://dbpedia.org/resource/Cincinnati>. ?geoPlace geo:name ?geoName. }").execSelect()
    ResultSetFormatter.outputAsJSON(new FileOutputStream(new File(GEO_JOIN_RESULT_NAME)),rs)
  }
}

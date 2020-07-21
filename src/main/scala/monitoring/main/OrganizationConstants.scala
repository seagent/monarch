package monitoring.main

object OrganizationConstants {
  /**
    * TODO: Sorgu federated yapılacak
    */
  val STOCK_QUERY_TEMPLATE: String = "SELECT ?nytCompany ?articleCount ?stockValue WHERE {" +
    "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany. " +
    "?nytCompany <http://data.nytimes.com/elements/associated_article_count> ?articleCount." +
    "?nytCompany <http://stockmarket.com/elements/stockValue> ?stockValue}"
  val FEDERATED_STOCK_QUERY_TEMPLATE:String ="SELECT  ?nytCompany ?articleCount ?stockValue WHERE {" +
    "SERVICE <http://155.223.25.4:8890/dbpedia/sparql> {" +
    "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany." +
    "}SERVICE <http://155.223.25.1:8890/nytimes/sparql> {" +
    "?nytCompany <http://data.nytimes.com/elements/associated_article_count> ?articleCount." +
    "}SERVICE <http://155.223.25.2:8890/stockmarket/sparql> {" +
    "?nytCompany <http://stockmarket.com/elements/stockValue> ?stockValue. }}"

  val RESOURCE_PATH = "src/main/resources/"
  val VOID_PATH: String = RESOURCE_PATH + "void/"
  val OWL_SAME_AS="http://www.w3.org/2002/07/owl#sameAs"

  def createStockQuery(companyURI: String, index: Int) = {
    s"""SELECT ?nytCompany$index ?articleCount$index ?stockValue$index WHERE {
       |<$companyURI> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany$index.
       |?nytCompany$index <http://data.nytimes.com/elements/associated_article_count> ?articleCount$index.
       |?nytCompany$index <http://stockmarket.com/elements/stockValue> ?stockValue$index}""".stripMargin
  }
}

package monitoring.main

object OrganizationConstants {
  /**
    * TODO: Sorgu federated yapılacak
    */
  val STOCK_QUERY_TEMPLATE: String = "SELECT ?nytCompany ?articleCount ?stockValue WHERE {" + "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?nytCompany. " + "?nytCompany <http://data.nytimes.com/elements/associated_article_count> ?articleCount." + "?nytCompany <http://stockmarket.com/elements/stockValue> ?stockValue}"

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

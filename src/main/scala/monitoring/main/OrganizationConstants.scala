package monitoring.main

object OrganizationConstants {
  /**
    * TODO: Sorgu federated yapılacak
    */
  val STOCK_QUERY_TEMPLATE: String = "SELECT ?sameCompany ?count ?stock WHERE {" + "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?sameCompany. " + "?sameCompany <http://data.nytimes.com/elements/associated_article_count> ?count." + "?sameCompany <http://stockmarket.com/elements/stockValue> ?value}"

  val RESOURCE_PATH = "src/main/resources/"
  val VOID_PATH: String = RESOURCE_PATH + "void/"
  val OWL_SAME_AS="http://www.w3.org/2002/07/owl#sameAs"

  def createStockQuery(companyURI: String, index: Int) = {
    s"""SELECT ?sameCompany$index ?count$index ?stock$index WHERE {
       |<$companyURI> <http://www.w3.org/2002/07/owl#sameAs> ?sameCompany$index.
       |?sameCompany$index <http://data.nytimes.com/elements/associated_article_count> ?count$index.
       |?sameCompany$index <http://stockmarket.com/elements/stockValue> ?value$index}""".stripMargin
  }
}

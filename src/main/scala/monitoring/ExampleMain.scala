package monitoring

import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory, ResultSetFormatter}
import com.hp.hpl.jena.rdf.model.ModelFactory
import monitoring.AgentApp.DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE
import monitoring.main.OrganizationConstants
import tr.edu.ege.seagent.wodqa.query.WodqaEngine

object ExampleMain {
  def main(args: Array[String]): Unit = {
    println(QueryFactory.create(OrganizationConstants.generateFederatedQueryWithMultipleSelection(1)))
    println(QueryFactory.create(OrganizationConstants.generateGenericFederatedQuery(1,"ALL","ALL","ALL")))
    println(QueryFactory.create(OrganizationConstants.generateGenericFederatedQuery(1,"2000","2000","2000")))
    println(QueryFactory.create(OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(1,"http://dbpedia.org/resource/company-1","3000", "3000")))
    println(QueryFactory.create(OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(1,"http://dbpedia.org/resource/company-1","ALL","ALL")))
    println(QueryFactory.create(OrganizationConstants.generateHighlySelectiveFederatedQuery(1)))
    /*val queryExecution = QueryExecutionFactory.create(OrganizationConstants.generateHighlySelectiveFederatedQuery(1), ModelFactory.createDefaultModel())
    val resultSet = queryExecution.execSelect()
    ResultSetFormatter.out(resultSet)
     */

    val query_count=20000
    val bunch_percent=10
    val bunch_count = query_count * bunch_percent / 100
    println(bunch_count)
    println(("0.0667".toDouble*15000).toInt)
    println(0.5D)

    //println(String.format(OrganizationConstants.FEDERATED_STOCK_QUERY_TEMPLATE,"http://dbpedia.org/resource/company-1"))
    //val query = QueryFactory.create(OrganizationConstants.generateFederatedQueryWithMultipleSelection(1))
    //print(query)
    //val resultSet = QueryExecutionFactory.create(query, ModelFactory.createDefaultModel()).execSelect()
    //ResultSetFormatter.outputAsJSON(resultSet)
    /*val wodqaEngine = new WodqaEngine()
    val resultSet = wodqaEngine.executeSelect(OrganizationConstants.generateFederatedQueryWithMultipleSelection(1))
    while(resultSet.hasNext){
      val solution = resultSet.next()
      println(solution)
    }*/



  }
}

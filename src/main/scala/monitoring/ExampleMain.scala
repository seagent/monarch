package monitoring

import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory, ResultSetFormatter}
import com.hp.hpl.jena.rdf.model.ModelFactory
import monitoring.main.OrganizationConstants
import tr.edu.ege.seagent.wodqa.query.WodqaEngine

object ExampleMain {
  def main(args: Array[String]): Unit = {
    /*println(QueryFactory.create(OrganizationConstants.generateFederatedQueryWithMultipleSelection(1)))
    println(QueryFactory.create(OrganizationConstants.generateGenericFederatedQuery(1,"ALL","ALL","ALL")))
    println(QueryFactory.create(OrganizationConstants.generateGenericFederatedQuery(1,"2000","2000","2000")))
    println(QueryFactory.create(OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(6001,"2000","2000", "2000")))
    println(QueryFactory.create(OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(5001,"ALL","ALL","ALL")))
*/
    println(QueryFactory.create(OrganizationConstants.generateSelectiveGenericFederatedQuery(5001,"MIN","MIN","MIN")))
    println(QueryFactory.create(OrganizationConstants.generateSelectiveGenericFederatedQuery(5001,"300","300","300")))
    println(QueryFactory.create(OrganizationConstants.generateSelectiveSpecificFederatedQuery(5001,"MIN","MIN","MIN")))
    println(QueryFactory.create(OrganizationConstants.generateSelectiveSpecificFederatedQuery(5001,"200","200","200")))
    /*val queryExecution = QueryExecutionFactory.create(OrganizationConstants.generateHighlySelectiveFederatedQuery(1), ModelFactory.createDefaultModel())
    val resultSet = queryExecution.execSelect()
    ResultSetFormatter.out(resultSet)
     */

    val query_count=20001
    val bunch_percent=10
    val bunch_count = query_count * bunch_percent / 100
    println(bunch_count)
    println(("0.01667".toDouble*60000).toInt)
    println(0.5D)

    println((query_count-1)%2000)

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

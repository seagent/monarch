package monitoring

import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory, ResultSetFormatter}
import com.hp.hpl.jena.rdf.model.ModelFactory
import monitoring.AgentApp.DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE
import monitoring.main.OrganizationConstants
import tr.edu.ege.seagent.wodqa.query.WodqaEngine

object ExampleMain {
  def main(args: Array[String]): Unit = {
    //println(OrganizationConstants.generateFederatedQueryWithMultipleSelection(1))
    //println(OrganizationConstants.generateGenericFederatedQuery(1,"ALL","ALL","ALL"))
    //println(OrganizationConstants.generateGenericFederatedQuery(1,"4500","4000","4000"))
    //println(OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(1,"http://dbpedia.org/resource/company-1","4000", "4000"))
    //println(OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(1,"http://dbpedia.org/resource/company-1","ALL","ALL"))
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

package actor

import com.hp.hpl.jena.query.QueryExecutionFactory
import com.hp.hpl.jena.rdf.model.ModelFactory
import monitoring.actor.SubQueryExecutor
import monitoring.main.MonitoringUtils
import monitoring.message.Result

class MockSubQueryExecutor extends SubQueryExecutor {

  override protected def executeQuery(query: String, endpoint: String): Result = {
    val qexec = QueryExecutionFactory.create(query, ModelFactory.createDefaultModel().read(endpoint, "TTL"))
    val res = qexec.execSelect()
    val result = MonitoringUtils.convertRdf2Result(res)
    qexec.close()
    result
  }
}

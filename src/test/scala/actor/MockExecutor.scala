package actor

import akka.actor.Cancellable
import com.hp.hpl.jena.query.ResultSetFactory
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import monitoring.actor.Executor
import monitoring.main.MonitoringUtils
import monitoring.message.{Result, ScheduledServiceClause}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class MockExecutor extends Executor {

  override protected def executeServiceClause(query: String, endpoint: String): Result = {
    val res = ResultSetFactory.load(endpoint, ResultsFormat.FMT_RS_JSON)
    val resultMock = MonitoringUtils.convertRdf2Result(res)
    Result(resultMock.resultJSON, resultMock.resultVars, endpoint.hashCode)
  }

  override protected def schedule(ssc: ScheduledServiceClause): Cancellable = {
    context.system.scheduler.schedule(5.seconds, 5.seconds, self, ssc)
  }
}

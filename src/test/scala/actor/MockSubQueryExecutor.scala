package actor

import akka.actor.Cancellable
import com.hp.hpl.jena.query.ResultSetFactory
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import monitoring.actor.SubQueryExecutor
import monitoring.main.MonitoringUtils
import monitoring.message.{ExecuteSubQuery, Result, ScheduledQuery}

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

class MockSubQueryExecutor extends SubQueryExecutor {

  override protected def executeQuery(query: String, endpoint: String): Result = {
    val res = ResultSetFactory.load(endpoint, ResultsFormat.FMT_RS_JSON)
    MonitoringUtils.convertRdf2Result(res)
  }

  override protected def schedule(sq: ScheduledQuery): Cancellable = {
    context.system.scheduler.schedule(5.seconds, 5.seconds, self, sq)
  }
}

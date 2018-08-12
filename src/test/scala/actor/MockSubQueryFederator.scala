package actor

import akka.actor.Props
import akka.cluster.sharding.ClusterSharding
import monitoring.actor.{SubQueryExecutor, SubQueryFederator}
import monitoring.message.ExecuteSubQuery
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

class MockSubQueryFederator extends SubQueryFederator {
  override protected def distribute(query: String, endpoints: Seq[String]): Unit = {
    endpoints foreach {
      endpoint =>
        val sqe = context.system.actorOf(Props(new MockSubQueryExecutor))
        sqe ! ExecuteSubQuery(query, endpoint)
    }
  }
}

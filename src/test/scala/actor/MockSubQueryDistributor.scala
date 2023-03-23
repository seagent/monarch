package actor

import akka.actor.Props
import akka.cluster.sharding.ClusterSharding
import monitoring.actor.{Executor, Distributor}
import monitoring.message.ExecuteSubQuery
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

class MockSubQueryDistributor extends Distributor {
  override protected def distribute(query: String, endpoints: Seq[String]): Unit = {
    endpoints foreach {
      endpoint =>
        val sqe = context.system.actorOf(Props(new MockSubQueryExecutor))
        sqe ! ExecuteSubQuery(query, endpoint)
    }
  }
}

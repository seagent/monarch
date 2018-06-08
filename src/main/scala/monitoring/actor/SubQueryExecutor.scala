package monitoring.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import monitoring.message.ExecuteSubQuery

object SubQueryExecutor {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case esq @ ExecuteSubQuery(_,_) => (esq.hashCode.toString, esq)
  }

  private val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case esq @ ExecuteSubQuery(_,_) => (esq.hashCode % numberOfShards).toString
  }
}

class SubQueryExecutor extends Actor with ActorLogging {
  override def receive: Receive = {
    case esq @ ExecuteSubQuery(query,endpoint)=> println(s"Hash Code: ${esq.hashCode}, Query Value: $query, Endpoint: $endpoint")
  }
}
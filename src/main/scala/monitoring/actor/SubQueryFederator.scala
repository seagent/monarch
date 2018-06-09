package monitoring.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import monitoring.message.{ExecuteSubQuery, FederateSubQuery}

object SubQueryFederator {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@FederateSubQuery(query, _) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateSubQuery(query, _) => (query.hashCode % numberOfShards).toString
  }
}

class SubQueryFederator extends Actor with ActorLogging {
  override def receive: Receive = {
    case fsq@FederateSubQuery(query, endpoints) =>
      log.info("Hash Code for Federate Sub Query: [{}], and Query Value: [{}], Endpoint Values: [{}]", fsq.hashCode, query, endpoints)
      endpoints foreach {
        endpoint =>
          val subQueryExecutorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryExecutor")
          subQueryExecutorRegion ! ExecuteSubQuery(query, endpoint)
      }
  }
}

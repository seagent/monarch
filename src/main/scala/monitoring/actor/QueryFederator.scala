package monitoring.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import akka.event.Logging
import monitoring.message.{FederateQuery, FederateSubQuery}

object QueryFederator {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@FederateQuery(query) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateQuery(query) => (query.hashCode % numberOfShards).toString
  }
}

class QueryFederator extends Actor with ActorLogging {

  override def receive: Receive = {
    case fq@FederateQuery(query) =>
      log.info("Hash Code for Federate Query: [{}], and Query Value: [{}]", fq.hashCode, query)
      val subQueryFederatorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryFederator")
      subQueryFederatorRegion ! FederateSubQuery(query, "endpoint-1" :: "endpoint-2" :: "endpoint-3" :: Nil)
  }
}

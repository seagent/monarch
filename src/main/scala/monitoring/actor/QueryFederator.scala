package monitoring.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import monitoring.message.FederateQuery

// Extract entity and shardId methods should be implemented

object QueryFederator{
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ FederateQuery(id, _) => (id.toString, msg)
  }

  private val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateQuery(id, _) => (id % numberOfShards).toString
  }
}

class QueryFederator extends Actor with ActorLogging{

  override def receive: Receive = {
    case FederateQuery(id,query) =>
      println(s"Query Id: $id, Query Value: $query")
  }
}

package monitoring.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import monitoring.message.FederateQuery

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
    case FederateQuery(query) =>
      println(s"Query Hash Code: ${query.hashCode}, Query Value: $query")
  }
}

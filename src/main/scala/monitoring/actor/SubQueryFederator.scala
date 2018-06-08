package monitoring.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import monitoring.message.FederateSubQuery

object SubQueryFederator{
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ FederateSubQuery(query,_) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateSubQuery(query,_) => (query.hashCode % numberOfShards).toString
  }
}

class SubQueryFederator extends Actor with ActorLogging{
  override def receive: Receive = {
    case FederateSubQuery(query,_)=> println(s"Query Hash Code: ${query.hashCode}, Query Value: $query")
  }
}

package monitoring.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ClusterSharding
import monitoring.message.{FederateQuery, Subscribe}

class SubscriberAgent extends Actor with ActorLogging {
  override def receive: Receive = {
    case Subscribe(query) =>
      val queryFederatorRegion = ClusterSharding.get(context.system).shardRegion("QueryFederator")
      queryFederatorRegion ! FederateQuery(query)
  }
}

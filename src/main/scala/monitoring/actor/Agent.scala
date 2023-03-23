package monitoring.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.client.ClusterClient
import monitoring.main.{Constants, RedisStore}
import monitoring.message.{FederateQuery, Register, Result}

object Agent {
  def props: Props = Props(new Agent)
}

class Agent extends Actor with ActorLogging {
  override def receive: Receive = {
    case register@Register(_, client) =>
      val federateQuery = new FederateQuery(register.query, "akka://Subscribing@155.223.25.4:2553/user/" + self.path.name)
      client ! ClusterClient.Send("/system/sharding/Federator", federateQuery, localAffinity = true)
      log.info("Federated query has been sent to the MonARCh")

    case result@Result(_, _, _) =>
      log.info("Result has been received. Current query count: [{}], and current actor count: [{}]", RedisStore.get(Constants.QUERY_COUNT).get, RedisStore.get(Constants.ACTOR_COUNT).get)
    case message@_ =>
      log.info("Received unknown message: [{}]", message)
  }
}

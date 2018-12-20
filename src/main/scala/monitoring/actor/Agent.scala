package monitoring.actor

import java.io.{File, FileOutputStream}

import akka.actor.{Actor, ActorLogging, ActorPath, Props}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import com.hp.hpl.jena.query.ResultSetFormatter
import monitoring.main.{Constants, DbUtils, MonitoringUtils, RedisStore}
import monitoring.message.{FederateQuery, Register, Result}

object Agent {
  def props: Props = Props(new Agent)
}

class Agent extends Actor with ActorLogging {
  override def receive: Receive = {
    case register@Register(_) =>
      val initialContacts = Set(
        ActorPath.fromString("akka://Monitoring@155.223.25.1:2551/system/receptionist"),
        ActorPath.fromString("akka://Monitoring@155.223.25.2:2551/system/receptionist"),
        ActorPath.fromString("akka://Monitoring@155.223.25.3:2551/system/receptionist"),
        ActorPath.fromString("akka://Monitoring@155.223.25.4:2551/system/receptionist"))
      val client = context.actorOf(ClusterClient.props(
        ClusterClientSettings(context.system).withInitialContacts(initialContacts)), "client")
      client ! ClusterClient.Send("/system/sharding/QueryFederator", FederateQuery(register.query), localAffinity = true)
    case result@Result(_, _, _) =>
      //ResultSetFormatter.out(result.toResultSet)
      log.info("Result key: [{}], result-value: [{}]", result.key, result.resultJSON.hashCode)
      log.info("Current query count: [{}], and current actor count: [{}]", RedisStore.get(Constants.QUERY_COUNT).get, RedisStore.get(Constants.ACTOR_COUNT).get)
  }
}

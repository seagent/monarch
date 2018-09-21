package monitoring.actor

import java.io.{File, FileOutputStream}

import akka.actor.{Actor, ActorLogging, ActorPath, Props}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import com.hp.hpl.jena.query.ResultSetFormatter
import monitoring.main.{DbUtils, MonitoringUtils}
import monitoring.message.{FederateQuery, Register, Result}

object Agent {
  def props: Props = Props(new Agent)
}

class Agent extends Actor with ActorLogging {
  override def receive: Receive = {
    case register@Register(_) =>
      val initialContacts = Set(
        ActorPath.fromString("akka://Monitoring@127.0.0.1:2551/system/receptionist"),
        ActorPath.fromString("akka://Monitoring@127.0.0.1:2552/system/receptionist"))
      val client = context.actorOf(ClusterClient.props(
        ClusterClientSettings(context.system).withInitialContacts(initialContacts)), "client")
      client ! ClusterClient.Send("/system/sharding/QueryFederator", FederateQuery(register.query), localAffinity = true)
    case result@Result(_, _, _) =>
      ResultSetFormatter.out(result.toResultSet)
      MonitoringUtils.printQueryCount
      MonitoringUtils.printActorCount
  }
}

package monitoring

import akka.actor.{ActorPath, ActorSystem, Props}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import com.typesafe.config.ConfigFactory
import monitoring.actor.SubscriberAgent
import monitoring.message.{FederateQuery, Subscribe}

object ClientApp {
    def main(args: Array[String]): Unit = {
      val config = ConfigFactory.parseString("akka.remote.artery.canonical.hostname = " + "127.0.0.1").
        withFallback(ConfigFactory.parseString("akka.remote.artery.canonical.port = " + 2553)).
        withFallback(ConfigFactory.load("agent.conf"))

      // Create an Akka system
      val system = ActorSystem("Subscribing", config)

      val initialContacts = Set(
        ActorPath.fromString("akka://Monitoring@127.0.0.1:2551/system/receptionist"),
        ActorPath.fromString("akka://Monitoring@127.0.0.1:2552/system/receptionist"))

      val client = system.actorOf(ClusterClient.props(
        ClusterClientSettings(system).withInitialContacts(initialContacts)), "client")
      client ! ClusterClient.Send("/system/sharding/QueryFederator", FederateQuery("subscribe-query"), localAffinity = true)

    }
}

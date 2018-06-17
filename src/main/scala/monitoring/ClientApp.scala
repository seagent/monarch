package monitoring

import akka.actor.{ActorPath, ActorSystem}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import com.typesafe.config.ConfigFactory
import monitoring.message.FederateQuery

object ClientApp {

  private val CROSS_DOMAIN_QUERY_3 = "SELECT ?president ?party ?page WHERE { SERVICE <http://localhost:7000/sparql/> {?president <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/President> . ?president <http://dbpedia.org/ontology/nationality> <http://dbpedia.org/resource/United_States> . ?president <http://dbpedia.org/ontology/party> ?party .} SERVICE <http://localhost:9000/sparql/> {?x <http://www.w3.org/2002/07/owl#sameAs> ?president . ?x <http://data.nytimes.com/elements/topicPage> ?page .}}"

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
    client ! ClusterClient.Send("/system/sharding/QueryFederator", FederateQuery(CROSS_DOMAIN_QUERY_3), localAffinity = true)

  }
}

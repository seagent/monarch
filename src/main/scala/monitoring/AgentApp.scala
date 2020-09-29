package monitoring

import java.net.{InetAddress, NetworkInterface}

import akka.actor.ActorSystem
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import com.typesafe.config.ConfigFactory
import monitoring.actor.Agent
import monitoring.main.OrganizationConstants
import monitoring.message.Register
import tr.edu.ege.seagent.wodqa.voiddocument.VoidModelConstructor

object AgentApp {

  private val DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE = "http://dbpedia.org/resource/company-"
  private val COMPANY_COUNT = 3500

  private val BUNCH_COUNT = 350

  def main(args: Array[String]): Unit = {
    //val organizationDataList = OrganizationDataReader.readOrganizationData("/organization_data.txt") "/home/burak/Development/monitoring-environment/resources/void"
    //val voidModel = VoidModelConstructor.constructVOIDSpaceModel(System.getProperty("user.dir") + "/src/main/resources/void")

    var ipAddress = getIpAddress
    var port = "2553"
    if (args.size > 1) {
      ipAddress = args(0)
      port = args(1)
    }
    val config = ConfigFactory.parseString("akka.remote.artery.canonical.hostname = " + ipAddress).
      withFallback(ConfigFactory.parseString("akka.remote.artery.canonical.port = " + port)).
      withFallback(ConfigFactory.load("agent.conf"))

    // Create an Akka system
    val system = ActorSystem("Subscribing", config)
    val client = system.actorOf(ClusterClient.props(ClusterClientSettings(system)), "client")
    var index = 0
    //val wodqaEngine = new WodqaEngine(true, false)
    for (outerIndex <- 1 to COMPANY_COUNT) {
      //for (orgData <- organizationDataList.asScala) {
      //index += 1
      //for (index <- 0 to COMPANY_COUNT) {
      val agent = system.actorOf(Agent.props, "Agent-" + outerIndex)
      //println(system.actorSelection("akka://Subscribing@172.17.0.1:2553/user/"+agent.path.name))
      //val rawQuery = OrganizationConstants.createStockQuery(orgData.getDbpediaCompany, outerIndex)
      //val rawQuery = String.format(OrganizationConstants.STOCK_QUERY_TEMPLATE, DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE + outerIndex)
      //val federatedQuery = wodqaEngine.federateQuery(voidModel, rawQuery, false)
      val federatedQuery = String.format(OrganizationConstants.FEDERATED_STOCK_QUERY_TEMPLATE, DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE + outerIndex)
      agent ! Register(federatedQuery, client)
      //}
      if (outerIndex % BUNCH_COUNT == 0) {
        Thread.sleep(60000)
      }
    }


  }

  private def getIpAddress: String = {
    val e = NetworkInterface.getNetworkInterfaces
    if (e.hasMoreElements) {
      val n = e.nextElement match {
        case e: NetworkInterface => e
        case _ => ???
      }
      val ee = n.getInetAddresses
      if (ee.hasMoreElements) {
        ee.nextElement match {
          case e: InetAddress => return e.getHostAddress
          case _ => ???
        }
      }
    }
    return "127.0.0.1"
  }
}

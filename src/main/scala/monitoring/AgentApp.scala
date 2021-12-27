package monitoring

import java.net.{InetAddress, NetworkInterface}
import akka.actor.ActorSystem
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import com.typesafe.config.ConfigFactory
import monitoring.actor.Agent
import monitoring.main.{Constants, OrganizationConstants, RedisStore}
import monitoring.message.Register
import tr.edu.ege.seagent.wodqa.voiddocument.VoidModelConstructor

object AgentApp {

  private val DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE = "http://dbpedia.org/resource/company-"

  def main(args: Array[String]): Unit = {
    //val organizationDataList = OrganizationDataReader.readOrganizationData("/organization_data.txt") "/home/burak/Development/monitoring-environment/resources/void"
    //val voidModel = VoidModelConstructor.constructVOIDSpaceModel(System.getProperty("user.dir") + "/src/main/resources/void")

    val ipAddress = if (args.isDefinedAt(0)) args(0) else getIpAddress
    val port = if (args.isDefinedAt(1)) args(1) else "2553"
    val query_count = if (args.isDefinedAt(2)) args(2).toInt else 5000
    val bunch_percent = if (args.isDefinedAt(3)) args(3).toDouble else 0.1D
    val query_selectivity = if (args.isDefinedAt(4)) args(4) else "HIGH"
    val selectionDbpedia = if (args.isDefinedAt(5)) args(5) else "ALL"
    val selectionNytimes = if (args.isDefinedAt(6)) args(6) else "ALL"
    val selectionStock = if (args.isDefinedAt(7)) args(7) else "ALL"
    val config = ConfigFactory.parseString("akka.remote.artery.canonical.hostname = " + ipAddress).
      withFallback(ConfigFactory.parseString("akka.remote.artery.canonical.port = " + port)).
      withFallback(ConfigFactory.load("agent.conf"))

    // Create an Akka system
    val system = ActorSystem("Subscribing", config)
    val client = system.actorOf(ClusterClient.props(ClusterClientSettings(system)), "client")
    val bunch_count = (query_count * bunch_percent).toInt
    for (index <- 1 to query_count) {
      val agent = system.actorOf(Agent.props, "Agent-" + index)

      val federatedQuery = query_selectivity match {
        case "VERY_HIGH" => OrganizationConstants.generateHighlySelectiveFederatedQuery(index)
        case "HIGH" => OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(index, DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE + index, selectionNytimes, selectionStock)
        case "MID" => OrganizationConstants.generateGenericFederatedQuery(index, selectionDbpedia, selectionNytimes, selectionStock)
        case "LOW" => OrganizationConstants.generateFederatedQueryWithMultipleSelection(index)
        case "MIX" => selectMixedFederatedQuery(query_count, index)

      }

      //val federatedQuery = String.format(OrganizationConstants.FEDERATED_STOCK_QUERY_TEMPLATE,DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE+index)
      agent ! Register(federatedQuery, client)
      if (index % (bunch_count) == 0) {
        //println("Index: "+index + ", bunch count: "+ bunch_count + ", Query count: "+RedisStore.get(Constants.QUERY_COUNT).get + ", Actor count: "+RedisStore.get(Constants.ACTOR_COUNT).get)
        Thread.sleep(60000)
      }
    }


  }

  private def selectMixedFederatedQuery(query_count: Int, index: Int) = {
    if (index <= query_count * 30 / 100) {
      OrganizationConstants.generateHighlySelectiveFederatedQuery(index)
    } else if (index > query_count * 30 / 100 && index <= query_count * 48 / 100) {
      OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(index, DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE + index, "1000", "1000")
    } else if (index > query_count * 48 / 100 && index <= query_count * 58 / 100) {
      OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(index, DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE + index, "2000", "2000")
    }
    else if (index > query_count * 58 / 100 && index <= query_count * 62 / 100) {
      OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(index, DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE + index, "3000", "3000")
    }
    else if (index > query_count * 62 / 100 && index <= query_count * 64 / 100) {
      OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(index, DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE + index, "4000", "4000")
    }
    else if (index > query_count * 64 / 100 && index <= query_count * 66 / 100) {
      OrganizationConstants.generateFederatedQueryForSpecificDbpediaCompany(index, DBPEDIA_COMPANY_RESOURCE_URI_TEMPLATE + index, "ALL", "ALL")
    }
    else if (index > query_count * 66 / 100 && index <= query_count * 72 / 100) {
      OrganizationConstants.generateGenericFederatedQuery(index, "1000", "1000", "1000")
    }
    else if (index > query_count * 72 / 100 && index <= query_count * 75 / 100) {
      OrganizationConstants.generateGenericFederatedQuery(index, "2000", "2000", "2000")
    }
    else if (index > query_count * 75 / 100 && index <= query_count * 77 / 100) {
      OrganizationConstants.generateGenericFederatedQuery(index, "3000", "3000", "3000")
    }
    else if (index > query_count * 77 / 100 && index <= query_count * 78 / 100) {
      OrganizationConstants.generateGenericFederatedQuery(index, "4000", "4000", "4000")
    }
    else if (index > query_count * 78 / 100 && index <= query_count * 79 / 100) {
      OrganizationConstants.generateGenericFederatedQuery(index, "ALL", "ALL", "ALL")
    }
    else if (index > query_count * 79 / 100 && index <= query_count * 80 / 100){
      OrganizationConstants.generateFederatedQueryWithMultipleSelection(index)
    }else{
      OrganizationConstants.generateHighlySelectiveFederatedQuery(index)
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

package monitoring

import java.net.{InetAddress, NetworkInterface}

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import monitoring.actor.Agent
import monitoring.main.{OrganizationConstants, OrganizationData, OrganizationDataReader}
import monitoring.message.Register
import tr.edu.ege.seagent.wodqa.query.WodqaEngine
import tr.edu.ege.seagent.wodqa.voiddocument.VoidModelConstructor

import collection.JavaConverters._

object AgentApp {

  def main(args: Array[String]): Unit = {
    val organizationDataList = OrganizationDataReader.readOrganizationData("/organization_data.txt")
    val voidModel = VoidModelConstructor.constructVOIDSpaceModel(System.getenv("HOME") +"/void")

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
    var index = 0
    for (orgData <- organizationDataList.asScala) {
      index += 1
      val agent = system.actorOf(Agent.props, "Agent-" + index)
      //println(system.actorSelection("akka://Subscribing@172.17.0.1:2553/user/"+agent.path.name))
      val rawQuery = String.format(OrganizationConstants.STOCK_QUERY_TEMPLATE, orgData.getDbpediaCompany)
      val wodqaEngine = new WodqaEngine(true, false)
      val federatedQuery = wodqaEngine.federateQuery(voidModel, rawQuery, false)
      agent ! Register(federatedQuery)
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

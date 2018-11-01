package monitoring

import java.net._

import akka.actor.{ActorSystem, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import com.typesafe.config.ConfigFactory
import monitoring.actor._
import monitoring.main.DbUtils

object App {
  def main(args: Array[String]): Unit = {
    DbUtils.deleteStore
    if (args.isEmpty)
      startup(Seq("2551", "2552"))
    else
      startup(args)
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

  def startup(args: Seq[String]): Unit = {
    // In a production application you wouldn't typically start multiple ActorSystem instances in the
    // same JVM, here we do it to easily demonstrate these ActorSytems (which would be in separate JVM's)
    // talking to each other.
    var ipAddress = getIpAddress
    var port = "2551"
    if (args.size > 1) {
      ipAddress = args(0)
      port = args(1)
    }
    // Override the configuration of the port
    val config = ConfigFactory.parseString("akka.remote.artery.canonical.hostname = " + ipAddress).
      withFallback(ConfigFactory.parseString("akka.remote.artery.canonical.port = " + port)).
      withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem("Monitoring", config)
    // Create an actor that starts the sharding and sends random messages

    val federatorRegion = ClusterSharding(system).start(
      typeName = "QueryFederator",
      entityProps = Props[QueryFederator],
      settings = ClusterShardingSettings(system),
      extractEntityId = QueryFederator.extractEntityId,
      extractShardId = QueryFederator.extractShardId)

    ClusterClientReceptionist(system).registerService(federatorRegion)

    ClusterSharding(system).start(
      typeName = "SubQueryFederator",
      entityProps = Props[SubQueryFederator],
      settings = ClusterShardingSettings(system),
      extractEntityId = SubQueryFederator.extractEntityId,
      extractShardId = SubQueryFederator.extractShardId)

    ClusterSharding(system).start(
      typeName = "SubQueryExecutor",
      entityProps = Props[SubQueryExecutor],
      settings = ClusterShardingSettings(system),
      extractEntityId = SubQueryExecutor.extractEntityId,
      extractShardId = SubQueryExecutor.extractShardId)

    ClusterSharding(system).start(
      typeName = "BucketDistributor",
      entityProps = Props[BucketDistributor],
      settings = ClusterShardingSettings(system),
      extractEntityId = BucketDistributor.extractEntityId,
      extractShardId = BucketDistributor.extractShardId)

    ClusterSharding(system).start(
      typeName = "HashJoinPerformer",
      entityProps = Props[HashJoinPerformer],
      settings = ClusterShardingSettings(system),
      extractEntityId = HashJoinPerformer.extractEntityId,
      extractShardId = HashJoinPerformer.extractShardId)


  }
}

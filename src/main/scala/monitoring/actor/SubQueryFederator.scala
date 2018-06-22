package monitoring.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import monitoring.message.{ExecuteSubQuery, FederateSubQuery, Result}

import scala.collection.immutable.HashMap


object SubQueryFederator {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@FederateSubQuery(query, _) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateSubQuery(query, _) => (query.hashCode % numberOfShards).toString
  }
}

class SubQueryFederator extends Actor with ActorLogging {

  private var resultMap: HashMap[FederateSubQuery, Result] = HashMap.empty
  private var registeryList: Vector[ActorRef] = Vector.empty

  override def receive: Receive = {
    case FederateSubQuery(query, endpoints) =>
      //log.info("Hash Code for Federate Sub Query: [{}], and Query Value: [{}], Endpoint Values: [{}]", fsq.hashCode, query, endpoints)
      distribute(query, endpoints)
      registerSender
    case result@Result(_) =>
      notifyRegisteryList(result)
  }

  private def distribute(query: String, endpoints: Seq[String]) = {
    endpoints foreach {
      endpoint =>
        val subQueryExecutorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryExecutor")
        subQueryExecutorRegion ! ExecuteSubQuery(query, endpoint)
    }
  }

  private def registerSender = {
    if (!registeryList.contains(sender)) {
      registeryList = registeryList :+ sender
    }
  }

  private def notifyRegisteryList(result: Result) = {
    registeryList foreach {
      registered => {
        registered ! result
      }
    }
  }

}

package monitoring.actor

import java.util

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import main.{DirectedQuery, QueryManager, Union}
import monitoring.message.{FederateQuery, FederateSubQuery, Result}

import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, HashMap}

object QueryFederator {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@FederateQuery(query) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateQuery(query) => (query.hashCode % numberOfShards).toString
  }
}

class QueryFederator extends Actor with ActorLogging {

  private val resultMap: HashMap[FederateQuery, Result] = HashMap.empty
  private val registeryList: ArrayBuffer[ActorRef] = ArrayBuffer.empty

  override def receive: Receive = {
    case fq@FederateQuery(query) =>
      log.info("Hash Code for Federate Query: [{}], and Query Value: [{}]", fq.hashCode, query)
      val subQueryFederatorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryFederator")
      val directedQueries = QueryManager.splitFederatedQuery(query, new util.ArrayList[Union])
      distribute(subQueryFederatorRegion, directedQueries)
      registerSender
    case result@Result(_) =>
      notifyRegisteryList(result)
  }

  private def distribute(subQueryFederatorRegion: ActorRef, directedQueries: util.List[DirectedQuery]) = {
    directedQueries forEach {
      directedQuery => {
        subQueryFederatorRegion ! FederateSubQuery(directedQuery.getQuery, directedQuery.getEndpoints.asScala)
      }
    }
  }

  private def registerSender = {
    if (!registeryList.contains(sender)) {
      registeryList += sender
    }
  }

  private def notifyRegisteryList(result: Result) = {
    registeryList foreach {
      parent => {
        parent ! result
      }
    }
  }
}

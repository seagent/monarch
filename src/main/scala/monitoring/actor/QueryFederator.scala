package monitoring.actor

import java.util

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import com.hp.hpl.jena.query.ResultSetFormatter
import main.{DirectedQuery, QueryManager, Union}
import monitoring.message.{DistributeBuckets, FederateQuery, FederateSubQuery, Result}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

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

  private var expectedCount = 0
  private val results: ArrayBuffer[Result] = ArrayBuffer.empty
  private val resultMap: mutable.HashMap[FederateQuery, Result] = mutable.HashMap.empty
  private val registeryList: mutable.ArrayBuffer[ActorRef] = mutable.ArrayBuffer.empty

  override def receive: Receive = {
    case fq@FederateQuery(query) =>
      log.info("Hash Code for Federate Query: [{}], and Query Value: [{}]", fq.hashCode, query)
      val subQueryFederatorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryFederator")
      val directedQueries = QueryManager.splitFederatedQuery(query, new util.ArrayList[Union])
      expectedCount = directedQueries.size
      distribute(subQueryFederatorRegion, directedQueries)
      registerSender
    case receivedResult@Result(_) =>
      val receivedRs = receivedResult.toResultSet()
      // get hash join performer region
      val bucketDistributorRegion = ClusterSharding.get(context.system).shardRegion("BucketDistributor")
      results foreach {
        result => {
          val innerRs = result.toResultSet()
          if (QueryManager.matchAnyVar(receivedRs.getResultVars, innerRs.getResultVars)) {
            results -= result
            bucketDistributorRegion ! DistributeBuckets(receivedResult, result)
            expectedCount -= 1
            break
          }
        }
      }
      results += receivedResult
      if (expectedCount == 0 && results.size == 1) {
        notifyRegisteryList(receivedResult)
        ResultSetFormatter.out(receivedRs)
        //log.info("Query Result has been completed as [{}]", receivedResult)
      }
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

package monitoring.actor

import java.util

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import com.hp.hpl.jena.query.ResultSetFormatter
import main.{DirectedQuery, QueryManager, Union}
import monitoring.message._

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap
import scala.util.control.Breaks._

object QueryFederator {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@FederateQuery(query) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateQuery(query) => (query.hashCode % numberOfShards).toString
  }
}

class QueryFederator extends Actor with ActorLogging {

  private var expectedCount = 0
  private var results: Vector[Result] = Vector.empty
  private var registeryList: Vector[ActorRef] = Vector.empty
  private var resultMap: HashMap[FederateQuery, Result] = HashMap.empty

  override def receive: Receive = {
    case fq@FederateQuery(query) =>
      log.info("Hash Code for Federate Query: [{}], and Query Value: [{}]", fq.hashCode, query)
      val subQueryFederatorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryFederator")
      val directedQueries = QueryManager.splitFederatedQuery(query, new util.ArrayList[Union])
      expectedCount = directedQueries.size - 1
      distribute(subQueryFederatorRegion, directedQueries)
      registerSender
    case receivedResult@Result(_) =>
      val receivedRs = receivedResult.toResultSet()
      // get hash join performer region
      val bucketDistributorRegion = ClusterSharding.get(context.system).shardRegion("BucketDistributor")
      var matched = false
      breakable {
        results foreach {
          result => {
            val innerRs = result.toResultSet()
            if (QueryManager.matchAnyVar(receivedRs.getResultVars, innerRs.getResultVars)) {
              results = results.filterNot(res => res == result)
              bucketDistributorRegion ! DistributeBuckets(receivedResult, result)
              expectedCount -= 1
              matched = true
              break
            }
          }
        }
      }
      if (!matched)
        results = results :+ receivedResult

      // if query completed print result
      if (expectedCount == 0 && results.size == 1)
        ResultSetFormatter.out(receivedRs)

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
      registeryList = registeryList :+ sender
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
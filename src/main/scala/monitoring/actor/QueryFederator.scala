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

  private var resultCount = 0
  private var results: Vector[Result] = Vector.empty
  private var registeryList: Vector[ActorRef] = Vector.empty
  private var resultMap: HashMap[Int, Result] = HashMap.empty
  private var queryResult: Option[Result] = None

  override def receive: Receive = {
    case fq@FederateQuery(query) =>
      log.info("Hash Code for Federate Query: [{}], and Query Value: [{}]", fq.hashCode, query)
      val subQueryFederatorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryFederator")
      val directedQueries = QueryManager.splitFederatedQuery(query, new util.ArrayList[Union])
      resultCount = directedQueries.size - 1
      distribute(subQueryFederatorRegion, directedQueries)
      registerSender
    case receivedResult@Result(_, _) =>
      resultMap += (receivedResult.hashCode() -> receivedResult)
      // get hash join performer region
      val bucketDistributorRegion = ClusterSharding.get(context.system).shardRegion("BucketDistributor")
      var matched = false
      breakable {
        results foreach {
          result => {
            if (QueryManager.matchAnyVar(receivedResult.resultVars.asJava, result.resultVars.asJava)) {
              results = results.filterNot(res => res == result)
              bucketDistributorRegion ! DistributeBuckets(receivedResult, result)
              resultCount -= 1
              matched = true
              break
            }
          }
        }
      }
      if (!matched)
        results = results :+ receivedResult

      // if query completed print result
      if (resultCount == 0 && results.size == 1) {
        queryResult = Some(receivedResult)
        notifyRegisteryList(receivedResult)
      }
    case rc@ResultChange(_) =>
      resultCount = resultMap.size - 1
      resultMap += (rc.result.hashCode() -> rc.result)
      results=resultMap.values.toVector
      val bucketDistributorRegion = ClusterSharding.get(context.system).shardRegion("BucketDistributor")
      breakable {
        results foreach {
          result => {
            if (QueryManager.matchAnyVar(rc.result.resultVars.asJava, result.resultVars.asJava)) {
              results = results.filterNot(res => res == result)
              bucketDistributorRegion ! DistributeBuckets(rc.result, result)
              resultCount -= 1
              break
            }
          }
        }
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
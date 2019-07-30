package monitoring.actor

import java.util

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import com.hp.hpl.jena.query.ResultSetFormatter
import main.{DirectedQuery, QueryManager, Union}
import monitoring.main.DbUtils
import monitoring.message._

import scala.collection.JavaConverters._
import scala.collection.immutable.{HashMap, Queue}

object QueryFederator {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@FederateQuery(query, _) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateQuery(query, _) => (query.hashCode % numberOfShards).toString
  }
}

class QueryFederator extends Actor with ActorLogging {

  private var resultCount = 0
  private var results: Vector[Result] = Vector.empty
  private var registeryList: Vector[String] = Vector.empty
  private var resultMap: HashMap[Int, Result] = HashMap.empty
  private var queryResult: Option[Result] = None
  private var federateQuery: Option[FederateQuery] = None

  override def preStart(): Unit = {
    super.preStart
    DbUtils.increaseActorCount
    log.info("Actor count has been increased")
  }

  override def postStop(): Unit = {
    super.postStop
    DbUtils.decreaseActorCount
    log.info("Actor count has been decreased")
  }

  override def receive: Receive = {
    case fq@FederateQuery(query, senderPath) =>
      federateQuery = Some(fq)
      DbUtils.incrementQueryCount(fq)
      log.debug("Hash Code for Federate Query: [{}], and Query Value: [{}]", fq.hashCode, query)
      registerSender(senderPath)
      if (queryResult.isDefined) {
        sender ! queryResult.get
      } else {
        federate(query)
      }
    case receivedResult@Result(_, _, _) =>
      // get hash join performer region
      processResult(receivedResult)
    case rc@ResultChange(_) =>
      applyChange(rc)
  }

  protected def federate(query: String): Unit = {
    val subQueryFederatorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryFederator")
    federate(query, subQueryFederatorRegion)
  }

  protected def federate(query: String, federator: ActorRef): Unit = {
    val directedQueries = QueryManager.splitFederatedQuery(query, new util.ArrayList[Union])
    distribute(federator, directedQueries)
  }

  protected def processResult(receivedResult: Result): Unit = {
    if (receivedResult.key != 1) {
      resultMap += (receivedResult.key -> receivedResult)
    }
    val matched = seekForMatch(receivedResult)
    if (!matched)
      results = results :+ receivedResult

    // if query completed print result
    if (resultCount == 0 && results.size == 1) {
      if (queryResult.isEmpty) {
        log.info("Result has been constructed for the federated query [{}]", federateQuery.get.query)
        notifyRegisteryList(receivedResult)
      } else if (!queryResult.contains(receivedResult)) {
        log.info("A result change has been detected for the federated query [{}]", federateQuery.get.query)
        notifyRegisteryList(receivedResult)
      }
      queryResult = Some(receivedResult)
    }
  }

  private def seekForMatch(receivedResult: Result): Boolean = {
    for {
      result <- results
      if QueryManager.matchAnyVar(receivedResult.resultVars.asJava, result.resultVars.asJava)
    } {
      results = results.filterNot(res => res == result)
      val bucketDistributor =context.actorOf(BucketDistributor.props)
      bucketDistributor ! DistributeBuckets(receivedResult, result)
      resultCount -= 1
      return true
    }
    return false
  }

  private def applyChange(resultChange: ResultChange) = {
    resultCount = resultMap.size - 1
    resultMap += (resultChange.result.key -> resultChange.result)
    results = resultMap.values.toVector.filterNot(res => res == resultChange.result)
    self ! resultChange.result
  }

  protected def distribute(subQueryFederatorRegion: ActorRef, directedQueries: util.List[DirectedQuery]) = {
    resultCount = directedQueries.size - 1
    directedQueries forEach {
      directedQuery => {
        directToSubQueryFederator(subQueryFederatorRegion, directedQuery)
      }
    }
  }

  protected def directToSubQueryFederator(subQueryFederatorRegion: ActorRef, directedQuery: DirectedQuery): Unit = {
    subQueryFederatorRegion ! FederateSubQuery(directedQuery.getQuery, directedQuery.getEndpoints.asScala)
  }

  private def registerSender(senderPath: String) = {
    if (!registeryList.contains(senderPath)) {
      registeryList = registeryList :+ senderPath
    }
  }

  private def notifyRegisteryList(result: Result) = {
    registeryList foreach {
      registered => {
        context.actorSelection(registered) ! result
        //log.info("Changed result has been sent to the agent [{}]", registered)
      }
    }
  }

}
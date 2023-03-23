package monitoring.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import main.{DirectedQuery, QueryManager, Union}
import monitoring.main.{DbUtils, MonitoringUtils}
import monitoring.message._
import org.apache.spark.util.SizeEstimator

import java.util
import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap

object Federator {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@FederateQuery(query, _) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateQuery(query, _) => (query.hashCode % numberOfShards).toString
  }
}

class Federator extends Actor with ActorLogging {

  private var resultCount = 0
  private var results: Vector[Result] = Vector.empty
  private var registeryList: Vector[String] = Vector.empty
  private var resultMap: HashMap[Int, Result] = HashMap.empty
  private var queryResult: Option[Result] = None
  private var federateQuery: Option[FederateQuery] = None
  private var startTimeInMillis = 0L;

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
      //log.info("Sender path: {}, self path {}",sender().path,self.path)
      startTimeInMillis = System.currentTimeMillis()
      federateQuery = Some(fq)
      DbUtils.incrementQueryCount(fq)
      val sizeInBytes = SizeEstimator.estimate(federateQuery)
      log.info("Size of the FederateQuery message sent from Agent to Federator is: [{}] Bytes, and is [{}]",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes))
      log.debug("Hash Code for Federate Query: [{}], and Query Value: [{}]", fq.hashCode, query)
      registerSender(senderPath)
      if (queryResult.isDefined) {
        sender ! queryResult.get
        val sizeInBytes = SizeEstimator.estimate(queryResult.get)
        log.info("Size of the contained result message sent from Federator to Agent is: [{}] Bytes, and is [{}]",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes))
      } else {
        federate(query)
      }
    case receivedResult@Result(_, _, _) =>
      // get hash join performer region
      processResult(receivedResult)
    case rc@ResultChange(_, _) =>
      applyChange(rc)
  }

  protected def federate(query: String): Unit = {
    val distributorRegion = ClusterSharding.get(context.system).shardRegion("Distributor")
    federate(query, distributorRegion)
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
      val sizeInBytes = SizeEstimator.estimate(receivedResult)
      if (queryResult.isEmpty) {
        log.info("Result has been constructed for the federated query [{}]", federateQuery.get.query)
        notifyRegisteryList(receivedResult)
        log.info("Federated query has been performed in: [{}] milliseconds", System.currentTimeMillis() - startTimeInMillis)
        log.info("Size of the new result message sent from Federator to Agent is: [{}] Bytes, and is [{}]. Message has been notified [{}] times",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes),registeryList.size)
      } else if (!queryResult.contains(receivedResult)) {
        log.info("A result change has been detected for the federated query [{}], and notified in [{}] milliseconds", federateQuery.get.query, System.currentTimeMillis() - startTimeInMillis)
        notifyRegisteryList(receivedResult)
        log.info("Size of the result change message sent from Federator to Agent is: [{}] Bytes, and is [{}]. Message has been notified [{}] times",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes),registeryList.size)
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
      val bucketDistributor = context.actorOf(ParallelJoinManager.props)
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
    startTimeInMillis = resultChange.detectionTime
    self ! resultChange.result
  }

  protected def distribute(distributorRegion: ActorRef, directedQueries: util.List[DirectedQuery]) = {
    resultCount = directedQueries.size - 1
    directedQueries forEach {
      directedQuery => {
        directToDistributor(distributorRegion, directedQuery)
      }
    }
  }

  protected def directToDistributor(distributorRegion: ActorRef, directedQuery: DirectedQuery): Unit = {
    val federateServiceClause = DistributeServiceClause(directedQuery.getQuery, directedQuery.getEndpoints.asScala)
    distributorRegion ! federateServiceClause
    val sizeInBytes = SizeEstimator.estimate(federateServiceClause)
    log.info("Size of the DistributeServiceClause message sent from Federator to Distributor is: [{}] Bytes, and is [{}]",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes))
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
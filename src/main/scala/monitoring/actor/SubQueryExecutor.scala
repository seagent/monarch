package monitoring.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.ShardRegion
import com.hp.hpl.jena.query.QueryExecutionFactory
import monitoring.main.{DbUtils, MonitoringUtils, OrganizationConstants}
import monitoring.message.{ExecuteSubQuery, Result, ResultChange, ScheduledQuery}
import org.apache.spark.util.SizeEstimator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SubQueryExecutor {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case esq@ExecuteSubQuery(_, _) => (esq.hashCode.toString, esq)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case esq@ExecuteSubQuery(_, _) => (esq.hashCode % numberOfShards).toString
  }
}

class SubQueryExecutor extends Actor with ActorLogging {

  private var register: Vector[ActorRef] = Vector.empty
  private var queryResult: Option[Result] = None

  override def preStart(): Unit = {
    super.preStart
    DbUtils.increaseActorCount
    log.debug("Actor count has been increased")
  }

  override def postStop(): Unit = {
    super.postStop
    DbUtils.decreaseActorCount
    log.debug("Actor count has been decreased")
  }

  override def receive: Receive = {
    case esq@ExecuteSubQuery(query, endpoint) =>
      //log.info("Sender path: {}, self path {}",sender().path,self.path)
      log.debug("Hash Code for Execute Sub Query: [{}], and Query Value: [{}], Endpoint Value: [{}]", esq.hashCode, query, endpoint)
      registerSender
      if (queryResult.isDefined) {
        sender ! queryResult.get
        val sizeInBytes = SizeEstimator.estimate(queryResult.get)
        log.info("Size of the contained result message sent from SubQueryExecutor to SubQueryDistributor is: [{}] Bytes, and is [{}]",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes))

      } else {
        val result = executeQuery(query, endpoint)
        queryResult = Some(result)
        sender ! queryResult.get
        //TODO: ilerde hashmap ten bakarak yapacaz ve endpointler ayrı graph olmalı
        if (!query.contains(OrganizationConstants.OWL_SAME_AS)) {
          schedule(ScheduledQuery(esq))
        }
      }
      val sizeInBytes = SizeEstimator.estimate(queryResult.get)
      log.info("Size of the new result message sent from SubQueryExecutor to SubQueryDistributor is: [{}] Bytes, and is [{}]",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes))

    case sq@ScheduledQuery(esq) =>
      //log.info("Hash Code for Scheduled Sub Query: [{}], and Scheduled Query Value: [{}]", sq.hashCode, esq)
      val result = executeQuery(esq.query, esq.endpoint)
      if (!queryResult.contains(result)) {
        log.info("A change has been detected for the sub-query [{}], and endpoint [{}]", esq.query, esq.endpoint)
        queryResult = Some(result)
        val resultChange = ResultChange(result, System.currentTimeMillis())
        notifyRegisteryList(resultChange)
        val sizeInBytes = SizeEstimator.estimate(resultChange)
        log.info("Size of the result change message sent from SubQueryExecutor to SubQueryDistributor is: [{}] Bytes, and is [{}]. Message has been notified [{}] times",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes),register.size)
      }
  }

  protected def schedule(sq: ScheduledQuery) = {
    context.system.scheduler.schedule(10.minutes, 10.minutes, self, sq)
  }

  private def notifyRegisteryList(message: AnyRef) = {
    register foreach {
      registered => {
        registered ! message
      }
    }
  }

  protected def executeQuery(query: String, endpoint: String) = {
    val execution = QueryExecutionFactory.sparqlService(endpoint, query)
    val result = MonitoringUtils.convertRdf2Result(execution.execSelect())
    execution.close()
    Result(result.resultJSON, result.resultVars, endpoint.hashCode)
  }


  private def registerSender = {
    if (!register.contains(sender) && sender != self) {
      register = register :+ sender
    }
  }

}
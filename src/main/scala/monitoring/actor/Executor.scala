package monitoring.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.ShardRegion
import com.hp.hpl.jena.query.QueryExecutionFactory
import monitoring.main.{DbUtils, MonitoringUtils}
import monitoring.message.{ExecuteServiceClause, Result, ResultChange, ScheduledServiceClause}
import org.apache.spark.util.SizeEstimator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Executor {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case esc@ExecuteServiceClause(_, _) => (esc.hashCode.toString, esc)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case esc@ExecuteServiceClause(_, _) => (esc.hashCode % numberOfShards).toString
  }
}

class Executor extends Actor with ActorLogging {

  private var register: Vector[ActorRef] = Vector.empty
  private var queryResult: Option[Result] = None
  private val DBPEDIA = "dbpedia"

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
    case esc@ExecuteServiceClause(query, endpoint) =>
      //log.info("Sender path: {}, self path {}",sender().path,self.path)
      log.debug("Hash Code for Execute SERVICE Clause: [{}], and Query Value: [{}], Endpoint Value: [{}]", esc.hashCode, query, endpoint)
      registerSender
      if (queryResult.isDefined) {
        sender ! queryResult.get
        val sizeInBytes = SizeEstimator.estimate(queryResult.get)
        log.info("Size of the contained result message sent from Executor to Distributor is: [{}] Bytes, and is [{}]",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes))

      } else {
        val result = executeServiceClause(query, endpoint)
        queryResult = Some(result)
        sender ! queryResult.get
        // dbpedia receives no update for this use case
        if (!endpoint.contains(DBPEDIA)) {
          schedule(ScheduledServiceClause(esc))
        }
      }
      val sizeInBytes = SizeEstimator.estimate(queryResult.get)
      log.info("Size of the new result message sent from Executor to Distributor is: [{}] Bytes, and is [{}]",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes))

    case ssc@ScheduledServiceClause(esc) =>
      //log.info("Hash Code for Scheduled SERVICE Clause: [{}], and Scheduled SERVICE Clause Value: [{}]", ssc.hashCode, esc)
      val result = executeServiceClause(esc.query, esc.endpoint)
      if (!queryResult.contains(result)) {
        log.info("A change has been detected for the SERVICE clause [{}], and endpoint [{}]", esc.query, esc.endpoint)
        queryResult = Some(result)
        val resultChange = ResultChange(result, System.currentTimeMillis())
        notifyRegisteryList(resultChange)
        val sizeInBytes = SizeEstimator.estimate(resultChange)
        log.info("Size of the result change message sent from Executor to Distributor is: [{}] Bytes, and is [{}]. Message has been notified [{}] times",sizeInBytes, MonitoringUtils.formatByteValue(sizeInBytes),register.size)
      }
  }

  protected def schedule(ssc: ScheduledServiceClause) = {
    context.system.scheduler.schedule(10.minutes, 10.minutes, self, ssc)
  }

  private def notifyRegisteryList(message: AnyRef) = {
    register foreach {
      registered => {
        registered ! message
      }
    }
  }

  protected def executeServiceClause(query: String, endpoint: String) = {
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
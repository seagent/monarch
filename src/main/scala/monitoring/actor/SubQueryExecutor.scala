package monitoring.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.ShardRegion
import com.hp.hpl.jena.query.QueryExecutionFactory
import monitoring.main.{DbUtils, MonitoringUtils}
import monitoring.message.{ExecuteSubQuery, Result, ResultChange, ScheduledQuery}

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
    log.info("Actor count has been increased")
  }

  override def postStop(): Unit = {
    super.postStop
    DbUtils.decreaseActorCount
    log.info("Actor count has been decreased")
  }

  override def receive: Receive = {
    case esq@ExecuteSubQuery(query, endpoint) =>
      log.debug("Hash Code for Execute Sub Query: [{}], and Query Value: [{}], Endpoint Value: [{}]", esq.hashCode, query, endpoint)
      registerSender
      if (queryResult.isDefined) {
        sender ! queryResult.get
      } else {
        val result = executeQuery(query, endpoint)
        queryResult = Some(result)
        sender ! queryResult.get
        schedule(ScheduledQuery(esq))
      }

    case sq@ScheduledQuery(esq) =>
      //log.info("Hash Code for Scheduled Sub Query: [{}], and Scheduled Query Value: [{}]", sq.hashCode, esq)
      val result = executeQuery(esq.query, esq.endpoint)
      if (!queryResult.contains(result)) {
        log.info("A change has been detected for the sub-query [{}], and endpoint [{}]", esq.query, esq.endpoint)
        queryResult = Some(result)
        notifyRegisteryList(ResultChange(result))
      }
  }

  protected def schedule(sq: ScheduledQuery) = {
    context.system.scheduler.schedule(2.minutes, 2.minutes, self, sq)
  }

  private def notifyRegisteryList(message: Any) = {
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
package monitoring.actor

import java.io.ByteArrayOutputStream

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.ShardRegion
import com.hp.hpl.jena.query.{QueryExecutionFactory, ResultSetFormatter}
import monitoring.message.{ExecuteSubQuery, Result, ResultChange}

import scala.collection.JavaConverters._
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

  override def receive: Receive = {
    case esq@ExecuteSubQuery(query, endpoint) =>
      log.info("Hash Code for Execute Sub Query: [{}], and Query Value: [{}], Endpoint Value: [{}]", esq.hashCode, query, endpoint)
      registerSender
      val result = executeQuery(query, endpoint)

      if (queryResult.isEmpty) {
        queryResult = Some(result)
        notifyRegisteryList(result)
        context.system.scheduler.schedule(0.seconds, 20.seconds, self, esq)
      }

      if (queryResult.isEmpty || !queryResult.contains(result)) {
        log.info("A change has been detected for the query [{}], and endpoint [{}]", query, endpoint)
        queryResult = Some(result)
        notifyRegisteryList(ResultChange(result))
      }

  }

  private def notifyRegisteryList(message: Any) = {
    register foreach {
      registered => {
        registered ! message
      }
    }
  }

  private def executeQuery(query: String, endpoint: String) = {
    val execution = QueryExecutionFactory.sparqlService(endpoint, query)
    val results = execution.execSelect
    val outputStream = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(outputStream, results)
    val json = new String(outputStream.toByteArray)
    execution.close
    Result(json, results.getResultVars.asScala)
  }

  private def registerSender = {
    if (!register.contains(sender) && sender != self) {
      register = register :+ sender
    }
  }

}
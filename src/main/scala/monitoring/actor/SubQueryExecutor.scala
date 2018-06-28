package monitoring.actor

import java.io.ByteArrayOutputStream

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.ShardRegion
import com.hp.hpl.jena.query.{QueryExecutionFactory, ResultSetFormatter}
import monitoring.message.{ExecuteSubQuery, Result}

import scala.collection.immutable.HashMap

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

  private var resultMap: HashMap[ExecuteSubQuery, Result] = HashMap.empty
  private var registeryList: Vector[ActorRef] = Vector.empty

  override def receive: Receive = {
    case esq@ExecuteSubQuery(query, endpoint) =>
      log.info("Hash Code for Execute Sub Query: [{}], and Query Value: [{}], Endpoint Value: [{}]", esq.hashCode, query, endpoint)
      val result = executeQuery(query, endpoint)
      resultMap += (esq -> result)
      registerSender
      notifyRegisteryList(result)
  }

  private def notifyRegisteryList(result: Result) = {
    registeryList foreach {
      registered => {
        registered ! result
      }
    }
  }

  private def executeQuery(query: String, endpoint: String) = {
    val execution = QueryExecutionFactory.sparqlService(endpoint, query)
    val results = execution.execSelect()
    val outputStream = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(outputStream, results)
    val json = new String(outputStream.toByteArray)
    execution.close
    Result(json)
  }

  private def registerSender = {
    if (!registeryList.contains(sender)) {
      registeryList = registeryList :+ sender
    }
  }

}
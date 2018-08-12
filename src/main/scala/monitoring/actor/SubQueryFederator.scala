package monitoring.actor

import java.io.ByteArrayOutputStream

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import com.hp.hpl.jena.query.{ResultSetFactory, ResultSetFormatter}
import com.hp.hpl.jena.sparql.engine.binding.Binding
import main.QueryIterCollection
import monitoring.message.{ExecuteSubQuery, FederateSubQuery, Result, ResultChange}

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap


object SubQueryFederator {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg@FederateSubQuery(query, _) => (query.hashCode.toString, msg)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case FederateSubQuery(query, _) => (query.hashCode % numberOfShards).toString
  }
}

class SubQueryFederator extends Actor with ActorLogging {

  private var registeryList: Vector[ActorRef] = Vector.empty
  private var resultCount = 0
  private var resultMap: HashMap[Int, Result] = HashMap.empty
  private var queryResult: Option[Result] = None

  override def receive: Receive = {
    case fsq@FederateSubQuery(query, endpoints) =>
      log.info("Hash Code for Federate Sub Query: [{}], and Query Value: [{}], Endpoint Values: [{}]", fsq.hashCode, query, endpoints)
      if (queryResult.isDefined) {
        sender ! queryResult.get
      } else {
        distribute(query, endpoints)
        registerSender
        resultCount = endpoints.size
      }
    case result@Result(_, _) =>
      resultCount -= 1
      resultMap += (result.hashCode -> result)
      if (resultCount == 0) {
        val finalRes = constructResult
        queryResult = Some(finalRes)
        notifyRegisteryList(finalRes)
      }
    case rc@ResultChange(_) =>
      resultMap += (rc.result.hashCode() -> rc.result)
      val newRes = constructResult
      if (!queryResult.contains(newRes))
        notifyRegisteryList(ResultChange(newRes))
      queryResult = Some(newRes)
  }

  private def constructResult = {
    val finalResultSet = ResultSetFactory.create(new QueryIterCollection(generateBindings.asJava), resultMap.values.head.resultVars.asJava)
    val outputStream = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(outputStream, finalResultSet)
    val finalResult = Result(new String(outputStream.toByteArray), finalResultSet.getResultVars.asScala)
    finalResult
  }

  private def generateBindings = {
    var bindingList: Vector[Binding] = Vector.empty
    for ((_, v) <- resultMap) {
      val rs = v.toResultSet
      while (rs.hasNext)
        bindingList = bindingList :+ rs.nextBinding
    }
    bindingList
  }

  protected def distribute(query: String, endpoints: Seq[String]) = {
    endpoints foreach {
      endpoint =>
        val subQueryExecutorRegion = ClusterSharding.get(context.system).shardRegion("SubQueryExecutor")
        subQueryExecutorRegion ! ExecuteSubQuery(query, endpoint)
    }
  }

  private def registerSender = {
    if (!registeryList.contains(sender)) {
      registeryList = registeryList :+ sender
    }
  }

  private def notifyRegisteryList(msg: Any) = {
    registeryList foreach {
      registered => {
        registered ! msg
      }
    }
  }

}

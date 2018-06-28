package monitoring.actor

import java.io.ByteArrayOutputStream

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import com.hp.hpl.jena.query.{ResultSetFactory, ResultSetFormatter}
import com.hp.hpl.jena.sparql.engine.binding.Binding
import main.QueryIterCollection
import monitoring.message.{ExecuteSubQuery, FederateSubQuery, Result}

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

  private var resultMap: HashMap[FederateSubQuery, Result] = HashMap.empty
  private var registeryList: Vector[ActorRef] = Vector.empty
  private var resultCount = 0
  private var bindingList: Vector[Binding] = Vector.empty

  override def receive: Receive = {
    case fsq@FederateSubQuery(query, endpoints) =>
      log.info("Hash Code for Federate Sub Query: [{}], and Query Value: [{}], Endpoint Values: [{}]", fsq.hashCode, query, endpoints)
      distribute(query, endpoints)
      registerSender
      resultCount = endpoints.size
    case result@Result(_) =>
      resultCount -= 1
      val resultSet = result.toResultSet()
      while (resultSet.hasNext)
        bindingList = bindingList :+ resultSet.nextBinding
      if (resultCount == 0) {
        val finalResultSet = ResultSetFactory.create(new QueryIterCollection(bindingList.asJava), resultSet.getResultVars)
        val outputStream = new ByteArrayOutputStream
        ResultSetFormatter.outputAsJSON(outputStream, finalResultSet)
        notifyRegisteryList(new Result(new String(outputStream.toByteArray)))
      }
  }

  private def distribute(query: String, endpoints: Seq[String]) = {
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

  private def notifyRegisteryList(result: Result) = {
    registeryList foreach {
      registered => {
        registered ! result
      }
    }
  }

}

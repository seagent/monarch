package monitoring.actor

import java.io.ByteArrayOutputStream

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.sharding.{ClusterSharding, ShardRegion}
import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory, ResultSetFormatter}
import com.hp.hpl.jena.sparql.core.Var
import com.hp.hpl.jena.sparql.engine.binding.Binding
import main.QueryIterCollection
import monitoring.message.{DistributeBuckets, PerformHashJoin, Result}
import tr.edu.ege.seagent.boundarq.filterbound.MultipleNode

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object BucketDistributor {
  val SPLIT_COUNT = 20

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case dbs@DistributeBuckets(_, _) => (dbs.hashCode.toString, dbs)
  }

  private val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case dbs@DistributeBuckets(_, _) => (dbs.hashCode % numberOfShards).toString
  }
}

class BucketDistributor extends Actor with ActorLogging {

  var bucketCount = 0
  val bindings: ArrayBuffer[Binding] = ArrayBuffer.empty

  override def receive: Receive = {

    case DistributeBuckets(firstRes, secondRes) =>
      // get hash join performer region
      val hashJoinRegion = ClusterSharding.get(context.system).shardRegion("HashJoinPerformer")

      // convert results to result sets
      val rsFirst = firstRes.toResultSet()
      val rsSecond = secondRes.toResultSet()

      // find common vars between result sets
      val commonVars = findCommonVars(rsFirst.getResultVars.asScala, rsSecond.getResultVars.asScala)

      // get bucket iterators
      val bucketIterFirst = generateBucketMap(rsFirst, commonVars).values.iterator
      val bucketIterSecond = generateBucketMap(rsSecond, commonVars).values.iterator

      // iterate over bucket iterators and perform hash join
      while (bucketIterFirst.hasNext && bucketIterSecond.hasNext) {
        performHashJoin(hashJoinRegion, rsFirst.getResultVars.asScala, rsSecond.getResultVars.asScala, bucketIterFirst, bucketIterSecond)
      }

    case result@Result(_) =>
      bucketCount -= 1
      val resultSet = result.toResultSet()
      insertResult(resultSet)
      if (bucketCount == 0) context.parent ! generateResult(resultSet.getResultVars.asScala, bindings)

  }

  def performHashJoin(hashJoinRegion: ActorRef, varsFirst: mutable.Buffer[String], varsSecond: mutable.Buffer[String], bucketIterFirst: Iterator[ArrayBuffer[Binding]], bucketIterSecond: Iterator[ArrayBuffer[Binding]]): Unit = {
    bucketCount += 1
    val resultFirst = generateResult(varsFirst, bucketIterFirst.next)
    val resultSecond = generateResult(varsSecond, bucketIterSecond.next)
    hashJoinRegion ! PerformHashJoin(resultFirst, resultSecond)
  }

  private def generateResult(vars: mutable.Buffer[String], bucket: ArrayBuffer[Binding]): Result = {
    val outputStream = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(outputStream, ResultSetFactory.create(new QueryIterCollection(bucket.asJava), vars.asJava))
    Result(new String(outputStream.toByteArray))
  }

  def findCommonVars(varsFirst: mutable.Buffer[String], varsSecond: mutable.Buffer[String]): mutable.ArrayBuffer[String] = {
    val commonVars: ArrayBuffer[String] = mutable.ArrayBuffer.empty
    varsFirst foreach {
      variable => {
        if (varsSecond.contains(variable)) {
          commonVars += variable
        }
      }
    }
    commonVars
  }

  def generateBucketMap(resultSet: ResultSet, commonVars: ArrayBuffer[String]): mutable.HashMap[Int, ArrayBuffer[Binding]] = {
    val bucketMap: mutable.HashMap[Int, ArrayBuffer[Binding]] = new mutable.HashMap[Int, ArrayBuffer[Binding]]() {
      override def default(key: Int) = new ArrayBuffer[Binding]()
    }

    while (resultSet.hasNext) {
      val binding = resultSet.nextBinding
      val multipleNode = getMultipleNode(commonVars, binding)
      val index = findIndex(multipleNode)
      bucketMap(index) += binding
    }

    bucketMap
  }

  private def findIndex(multipleNode: MultipleNode) = {
    var index = multipleNode.hashCode % BucketDistributor.SPLIT_COUNT
    if (index < 0) index += BucketDistributor.SPLIT_COUNT
    index
  }

  def getMultipleNode(commonVars: ArrayBuffer[String], binding: Binding): MultipleNode = {
    val multipleNode = new MultipleNode
    for (commonVar <- commonVars) {
      multipleNode.add(binding.get(Var.alloc(commonVar)))
    }
    multipleNode
  }

  private def insertResult(resultSet: ResultSet): Unit = {
    while (resultSet.hasNext) {
      bindings += resultSet.nextBinding
    }
  }

}

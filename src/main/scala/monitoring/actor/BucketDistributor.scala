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
import scala.collection.immutable.HashMap
import scala.collection.mutable

object BucketDistributor {

  val splitCount = 20

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case dbs@DistributeBuckets(_, _) => (dbs.hashCode.toString, dbs)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case dbs@DistributeBuckets(_, _) => (dbs.hashCode % numberOfShards).toString
  }
}

class BucketDistributor extends Actor with ActorLogging {

  private var bucketCount = 0
  private var bindings: Vector[Binding] = Vector.empty
  private var registeryList: Vector[ActorRef] = Vector.empty

  override def receive: Receive = {

    case DistributeBuckets(firstRes, secondRes) =>
      distributeBuckets(firstRes, secondRes)

    case result@Result(_) =>
      handleJoinResult(result)

  }

  private def handleJoinResult(result: Result) = {
    bucketCount -= 1
    val resultSet = result.toResultSet()
    insertResult(resultSet)
    // if join has completed notify join result
    if (bucketCount == 0) {
      val result = generateResult(resultSet.getResultVars.asScala, bindings)
      notifyRegisteryList(result)
    }
  }

  private def distributeBuckets(firstRes: Result, secondRes: Result) = {
    // get hash join performer region
    val hashJoinRegion = ClusterSharding.get(context.system).shardRegion("HashJoinPerformer")

    registerSender

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
  }

  def performHashJoin(hashJoinRegion: ActorRef, varsFirst: mutable.Buffer[String], varsSecond: mutable.Buffer[String], bucketIterFirst: Iterator[Vector[Binding]], bucketIterSecond: Iterator[Vector[Binding]]): Unit = {
    bucketCount += 1
    val resultFirst = generateResult(varsFirst, bucketIterFirst.next)
    val resultSecond = generateResult(varsSecond, bucketIterSecond.next)
    hashJoinRegion ! PerformHashJoin(resultFirst, resultSecond)
  }

  private def generateResult(vars: mutable.Buffer[String], bucket: Vector[Binding]): Result = {
    val outputStream = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(outputStream, ResultSetFactory.create(new QueryIterCollection(bucket.asJava), vars.asJava))
    Result(new String(outputStream.toByteArray))
  }

  def findCommonVars(varsFirst: mutable.Buffer[String], varsSecond: mutable.Buffer[String]): Vector[String] = {
    var commonVars: Vector[String] = Vector.empty
    varsFirst foreach {
      variable => {
        if (varsSecond.contains(variable)) {
          commonVars = commonVars :+ variable
        }
      }
    }
    commonVars
  }

  def generateBucketMap(resultSet: ResultSet, commonVars: Vector[String]): HashMap[Int, Vector[Binding]] = {
    var bucketMap: HashMap[Int, Vector[Binding]] = HashMap.empty

    for (i <- 0 until BucketDistributor.splitCount) {
      bucketMap += (i -> Vector.empty[Binding])
    }

    while (resultSet.hasNext) {
      val binding = resultSet.nextBinding
      val multipleNode = getMultipleNode(commonVars, binding)
      val index = findIndex(multipleNode)
      val bindings = bucketMap(index)
      val newBindings = bindings :+ binding
      bucketMap += (index -> newBindings)
    }

    bucketMap
  }

  private def findIndex(multipleNode: MultipleNode) = {
    var index = multipleNode.hashCode % BucketDistributor.splitCount
    if (index < 0) index += BucketDistributor.splitCount
    index
  }

  def getMultipleNode(commonVars: Vector[String], binding: Binding): MultipleNode = {
    val multipleNode = new MultipleNode
    for (commonVar <- commonVars) {
      multipleNode.add(binding.get(Var.alloc(commonVar)))
    }
    multipleNode
  }

  private def insertResult(resultSet: ResultSet): Unit = {
    while (resultSet.hasNext) {
      bindings = bindings :+ resultSet.nextBinding
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
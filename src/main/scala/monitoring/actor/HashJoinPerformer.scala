package monitoring.actor

import java.io.ByteArrayOutputStream

import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import com.hp.hpl.jena.query.ResultSetFormatter
import main.ResultSetMerger
import monitoring.message.{DistributeBuckets, PerformHashJoin, Result}

object HashJoinPerformer {
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case phj@PerformHashJoin(_, _) => (phj.hashCode.toString, phj)
  }

  private val numberOfShards = 20

  val extractShardId: ShardRegion.ExtractShardId = {
    case phj@PerformHashJoin(_, _) => (phj.hashCode % numberOfShards).toString
  }
}

class HashJoinPerformer extends Actor with ActorLogging {
  override def receive: Receive = {
    case PerformHashJoin(firstRs, secondRs) =>
      val resultSet = new ResultSetMerger().mergeResultSets(firstRs.toResultSet(), secondRs.toResultSet())
      //serialize result set
      val outputStream = new ByteArrayOutputStream
      ResultSetFormatter.outputAsJSON(outputStream, resultSet)
      //send hash join result back to the sender
      sender ! Result(new String(outputStream.toByteArray))
  }
}

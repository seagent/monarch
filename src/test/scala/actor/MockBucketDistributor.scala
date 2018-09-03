package actor

import akka.actor.Props
import monitoring.actor.{BucketDistributor, HashJoinPerformer}
import monitoring.message.Result

class MockBucketDistributor extends BucketDistributor {
  override protected def distributeBuckets(firstRes: Result, secondRes: Result): Unit = {
    val hjp = context.system.actorOf(Props(new HashJoinPerformer))
    performDistribution(hjp, firstRes, secondRes)
  }
}

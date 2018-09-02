import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class HashJoinPerformerTest extends TestKit(ActorSystem("SubQueryFederatorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Hash Join Performer Actor" must {

    "perform hash join and return join result back to its sender" in {

    }
  }
}

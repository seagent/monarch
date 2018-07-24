import actor.MockSubQueryExecutor
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import monitoring.message.{ExecuteSubQuery, Result}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class SubQueryExecutorTest extends TestKit(ActorSystem("SubQueryExecutorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

// In order to handle result change, some query result should be stored locally and changed some data in it.
  "An Executor actor" must {

    "execute query and return result to its register list" in {
      val sqe = system.actorOf(Props(new MockSubQueryExecutor))
      sqe ! ExecuteSubQuery("", "")
      expectMsg(Result("",Vector.empty[String]))
    }

  }

}

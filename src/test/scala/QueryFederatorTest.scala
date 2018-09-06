import TestUtils.RESULT_FILE_NAME
import actor.{MockQueryFederator, MockSubQueryExecutor}
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory}
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import monitoring.ClientApp
import monitoring.main.MonitoringUtils
import monitoring.message.{ExecuteSubQuery, FederateQuery, Result, ResultChange}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class QueryFederatorTest extends TestKit(ActorSystem("QueryFederatorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Query Federator actor" must {

    "federate query and return result to its register list" in {

      val probe = TestProbe()

      // create a new actor
      val qf = system.actorOf(Props(new MockQueryFederator))
      probe watch qf
      // send execute sub query message
      qf ! FederateQuery(ClientApp.GOOD_LOOKING_QUERY)

      //create expected message instance
      val expectedResult = createExpectedResult(TestUtils.DBPEDIA_LMDB_GEO_JOIN_RESULT_NAME)
      // check if received message is the expected one
      expectMsg(expectedResult)
      // kill actor instance
      qf ! PoisonPill
      probe.expectTerminated(qf)
    }

  }

  private def createExpectedResult(endpoint: String) = {
    val rsExp = ResultSetFactory.load(endpoint, ResultsFormat.FMT_RS_JSON)
    val result = MonitoringUtils.convertRdf2Result(rsExp)
    Result(result.resultJSON, result.resultVars, 1)
  }
}

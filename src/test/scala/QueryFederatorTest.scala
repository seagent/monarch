import TestUtils.RESULT_FILE_NAME
import actor.{MockQueryFederator, MockSubQueryExecutor, MockSubQueryFederator}
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory}
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import monitoring.ClientApp
import monitoring.main.MonitoringUtils
import monitoring.message._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.libs.json.Json

import scala.concurrent.duration._

class QueryFederatorTest extends TestKit(ActorSystem("QueryFederatorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Query Federator actor" must {

    "federate query and return result to its register list" in {

      TestUtils.changeFileInto(TestUtils.GEO_ORIGINAL_RESULT_NAME, TestUtils.GEO_JOIN_RESULT_NAME)

      val probe = TestProbe()

      // create a new actor
      val qf = system.actorOf(Props(new MockQueryFederator))
      probe watch qf
      // send execute sub query message
      qf ! FederateQuery(ClientApp.GOOD_LOOKING_QUERY)

      //create expected message instance
      val expectedResult = createExpectedResult(TestUtils.DBPEDIA_LMDB_GEO_JOIN_RESULT_NAME, -657188017)
      // check if received message is the expected one
      expectMsg(expectedResult)
      // kill actor instance
      qf ! PoisonPill
      probe.expectTerminated(qf)
    }

    "notify change to its register list" in {
      TestUtils.changeFileInto(TestUtils.GEO_ORIGINAL_RESULT_NAME, TestUtils.GEO_JOIN_RESULT_NAME)
      val probe = TestProbe()

      // create a new actor
      val qf = system.actorOf(Props(new MockQueryFederator))
      probe watch qf
      // send execute sub query message
      qf ! FederateQuery(ClientApp.GOOD_LOOKING_QUERY)

      //create expected message instance
      val expectedResult = createExpectedResult(TestUtils.DBPEDIA_LMDB_GEO_JOIN_RESULT_NAME, -657188017)
      // check if received message is the expected one
      expectMsg(expectedResult)

      TestUtils.changeFileInto(TestUtils.GEO_CHANGED_RESULT_NAME, TestUtils.GEO_JOIN_RESULT_NAME)
      // create expected message instance
      val expectedChangedResult = createExpectedResult(TestUtils.TRIPLE_JOIN_CHANGED_RESULT_NAME, 1918268440)
      // check if received result is result change message
      expectMsg(10.seconds, expectedChangedResult)
      // kill actor instance
      qf ! PoisonPill
      probe.expectTerminated(qf)
    }

    "return same result if it is not changed and not notify any change" in {
      TestUtils.changeFileInto(TestUtils.GEO_ORIGINAL_RESULT_NAME, TestUtils.GEO_JOIN_RESULT_NAME)
      val probe = TestProbe()

      // create a new actor
      val qf = system.actorOf(Props(new MockQueryFederator))
      probe watch qf
      // send execute sub query message
      qf ! FederateQuery(ClientApp.GOOD_LOOKING_QUERY)

      //create expected message instance
      val expectedResult = createExpectedResult(TestUtils.DBPEDIA_LMDB_GEO_JOIN_RESULT_NAME, -657188017)
      expectMsg(expectedResult)
      // send same query again
      qf ! FederateQuery(ClientApp.GOOD_LOOKING_QUERY)
      // expect same result
      expectMsg(expectedResult)
      // kill actor instance
      qf ! PoisonPill
      probe.expectTerminated(qf)
    }


  }

  private def createExpectedResult(endpoint: String, expectedKey: Int) = {
    val rsExp = ResultSetFactory.load(endpoint, ResultsFormat.FMT_RS_JSON)
    val result = MonitoringUtils.convertRdf2Result(rsExp)
    Result(result.resultJSON, result.resultVars, expectedKey)
  }
}

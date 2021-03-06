import java.io.{File, PrintWriter}

import TestUtils.RESULT_FILE_NAME
import actor.MockSubQueryExecutor
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory}
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import monitoring.main.MonitoringUtils
import monitoring.message.{ExecuteSubQuery, Result, ResultChange}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class SubQueryExecutorTest extends TestKit(ActorSystem("SubQueryExecutorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An Executor actor" must {

    "execute query and return result to its register list" in {

      val probe = TestProbe()

      // create a new actor
      val sqe = system.actorOf(Props(new MockSubQueryExecutor))
      probe watch sqe
      // send execute sub query message
      sqe ! ExecuteSubQuery(TestUtils.DBPEDIA_DIRECTOR_SELECT_QUERY, TestUtils.RESULT_FILE_NAME)

      //create expected message instance
      val rsExp = ResultSetFactory.load(TestUtils.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      val expectedResult = createExpectedResult(rsExp, TestUtils.RESULT_FILE_NAME)
      // check if received message is the expected one
      expectMsg(expectedResult)
      // kill actor instance
      sqe ! PoisonPill
      probe.expectTerminated(sqe)
    }

    "notify a change to its register list" in {

      TestUtils.changeFileInto(TestUtils.ACTUAL_RESULT_FILE_NAME, TestUtils.RESULT_FILE_NAME)

      val probe = TestProbe()

      // create changed result file by modifying actual result
      val changedJsonText = TestUtils.changeResultFile(TestUtils.RESULT_FILE_NAME)

      // create a sub query executor actor
      val sqe = system.actorOf(Props(new MockSubQueryExecutor))
      probe watch sqe
      // send an execute sub query message
      sqe ! ExecuteSubQuery(TestUtils.DBPEDIA_DIRECTOR_SELECT_QUERY, TestUtils.RESULT_FILE_NAME)
      // first expect a Result message
      val rsExp = ResultSetFactory.load(TestUtils.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      // assert expected message
      expectMsg(createExpectedResult(rsExp, TestUtils.RESULT_FILE_NAME))
      // modify the registered result
      TestUtils.write2File(changedJsonText, RESULT_FILE_NAME)
      // create expected message instance
      val rsChanged = ResultSetFactory.load(TestUtils.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      val resultChangeMsgExp = ResultChange(createExpectedResult(rsChanged, TestUtils.RESULT_FILE_NAME),0)
      // check if received result is result change message
      expectMsg(10.seconds, resultChangeMsgExp)
      // kill actor instance
      sqe ! PoisonPill
      probe.expectTerminated(sqe)
    }

    "return same result if it is not changed and not notify any change" in {
      val probe = TestProbe()

      // arrange the result file as expected
      TestUtils.changeFileInto(TestUtils.ACTUAL_RESULT_FILE_NAME, RESULT_FILE_NAME)
      // create a new actor
      val sqe = system.actorOf(Props(new MockSubQueryExecutor))
      probe watch sqe
      // send execute sub query message
      sqe ! ExecuteSubQuery(TestUtils.DBPEDIA_DIRECTOR_SELECT_QUERY, TestUtils.RESULT_FILE_NAME)

      //create expected message instance
      val rsExp = ResultSetFactory.load(TestUtils.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      val expectedResult = createExpectedResult(rsExp, TestUtils.RESULT_FILE_NAME)
      // check if received message is the expected one
      expectMsg(expectedResult)
      // send same query again
      sqe ! ExecuteSubQuery(TestUtils.DBPEDIA_DIRECTOR_SELECT_QUERY, TestUtils.RESULT_FILE_NAME)
      // expect same result
      expectMsg(expectedResult)
      // kill actor instance
      sqe ! PoisonPill
      probe.expectTerminated(sqe)
    }

  }

  private def createExpectedResult(rsExp: ResultSet, endpoint: String) = {
    val result = MonitoringUtils.convertRdf2Result(rsExp)
    val result2 = Result(result.resultJSON, result.resultVars, endpoint.hashCode)
    result2
  }
}

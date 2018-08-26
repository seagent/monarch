import actor.MockSubQueryFederator
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory}
import com.hp.hpl.jena.sparql.engine.binding.Binding
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import main.QueryIterCollection
import monitoring.main.MonitoringUtils
import monitoring.message.{FederateSubQuery, ResultChange}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class SubQueryFederatorTest extends TestKit(ActorSystem("SubQueryFederatorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll{

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A SubQueryFederator actor" must {

    "federate a sub query and return result to the sender" in {
      val probe = TestProbe()
      // create a new actor
      val sqf = system.actorOf(Props(new MockSubQueryFederator))
      probe watch sqf
      sqf ! FederateSubQuery(TestUtils.PERSON_SELECT_QUERY, Vector(TestUtils.DBPEDIA_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME))

      // check if received message is the expected one
      val expectedResult = generateExpectedResult
      expectMsg(expectedResult)
      // kill actor instance
      sqf ! PoisonPill
      probe.expectTerminated(sqf)
    }

    "notify change to its register list" in {
      TestUtils.cleanUpResultFile(TestUtils.ACTUAL_DBPEDIA_RESULT_FILE_NAME, TestUtils.DBPEDIA_RESULT_FILE_NAME)
      TestUtils.cleanUpResultFile(TestUtils.ACTUAL_IMDB_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME)
      val probe = TestProbe()

      // create changed result file by modifying actual result
      val changedJsonText = TestUtils.changeResultFile(TestUtils.IMDB_RESULT_FILE_NAME)

      // create a new actor
      val sqf = system.actorOf(Props(new MockSubQueryFederator))
      probe watch sqf
      sqf ! FederateSubQuery(TestUtils.PERSON_SELECT_QUERY, Vector(TestUtils.DBPEDIA_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME))
      // check if received message is the expected one
      expectMsg(generateExpectedResult)
      TestUtils.write2File(changedJsonText, TestUtils.IMDB_RESULT_FILE_NAME)
      // create expected message instance
      val resultChangeMsgExp = ResultChange(generateExpectedResult)
      // check if received result is result change message
      expectMsg(10.seconds, resultChangeMsgExp)
      // kill actor instance
      sqf ! PoisonPill
      probe.expectTerminated(sqf)
    }

    "return same result if it is not changed and not notify any change" in {
      val probe = TestProbe()

      // arrange the result file as expected
      TestUtils.cleanUpResultFile(TestUtils.ACTUAL_DBPEDIA_RESULT_FILE_NAME, TestUtils.DBPEDIA_RESULT_FILE_NAME)
      TestUtils.cleanUpResultFile(TestUtils.ACTUAL_IMDB_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME)
      // create a new actor
      val sqf = system.actorOf(Props(new MockSubQueryFederator))
      probe watch sqf
      sqf ! FederateSubQuery(TestUtils.PERSON_SELECT_QUERY, Vector(TestUtils.DBPEDIA_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME))

      //create expected message instance
      val expectedResult = generateExpectedResult
      expectMsg(expectedResult)
      // send same query again
      sqf ! FederateSubQuery(TestUtils.PERSON_SELECT_QUERY, Vector(TestUtils.DBPEDIA_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME))
      // expect same result
      expectMsg(expectedResult)
      // kill actor instance
      sqf ! PoisonPill
      probe.expectTerminated(sqf)
    }
  }

  private def generateExpectedResult = {
    val imdbRes = ResultSetFactory.load(TestUtils.IMDB_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
    var bindings = generateBindings(imdbRes)
    val dbpediaRes = ResultSetFactory.load(TestUtils.DBPEDIA_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
    bindings ++= generateBindings(dbpediaRes)
    val expectedRes = constructResult(bindings, dbpediaRes.getResultVars)
    expectedRes
  }

  private def constructResult(bindingList: Vector[Binding], resVars: java.util.List[String]) = {
    val finalResultSet = ResultSetFactory.create(new QueryIterCollection(bindingList.asJava), resVars)
    MonitoringUtils.convertRdf2Result(finalResultSet)
  }

  def generateBindings(resultSet: ResultSet) = {
    var bindingList: Vector[Binding] = Vector.empty
    while (resultSet.hasNext)
      bindingList = bindingList :+ resultSet.nextBinding
    bindingList
  }

}

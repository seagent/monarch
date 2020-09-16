import actor.MockSubQueryDistributor
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory}
import com.hp.hpl.jena.sparql.engine.binding.Binding
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import main.QueryIterCollection
import monitoring.main.MonitoringUtils
import monitoring.message.{FederateSubQuery, Result, ResultChange}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class SubQueryDistributorTest extends TestKit(ActorSystem("SubQueryDistributorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A SubQueryDistributor actor" must {

    val fsq = FederateSubQuery(TestUtils.PERSON_SELECT_QUERY, Vector(TestUtils.DBPEDIA_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME))
    "federate a sub query and return result to the sender" in {
      val probe = TestProbe()
      // create a new actor
      val sqf = system.actorOf(Props(new MockSubQueryDistributor))
      probe watch sqf
      sqf ! fsq

      // check if received message is the expected one
      val expectedResult = generateExpectedResult(fsq.hashCode)
      expectMsg(expectedResult)
      // kill actor instance
      sqf ! PoisonPill
      probe.expectTerminated(sqf)
    }

    "notify change to its register list" in {
      TestUtils.changeFileInto(TestUtils.ACTUAL_DBPEDIA_RESULT_FILE_NAME, TestUtils.DBPEDIA_RESULT_FILE_NAME)
      TestUtils.changeFileInto(TestUtils.ACTUAL_IMDB_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME)
      val probe = TestProbe()

      // create changed result file by modifying actual result
      val changedJsonText = TestUtils.changeResultFile(TestUtils.IMDB_RESULT_FILE_NAME)

      // create a new actor
      val sqf = system.actorOf(Props(new MockSubQueryDistributor))
      probe watch sqf
      sqf ! fsq
      // check if received message is the expected one
      expectMsg(generateExpectedResult(fsq.hashCode))
      TestUtils.write2File(changedJsonText, TestUtils.IMDB_RESULT_FILE_NAME)
      // create expected message instance
      val resultChangeMsgExp = ResultChange(generateExpectedResult(fsq.hashCode),0)
      // check if received result is result change message
      expectMsg(10.seconds, resultChangeMsgExp)
      // kill actor instance
      sqf ! PoisonPill
      probe.expectTerminated(sqf)
    }

    "return same result if it is not changed and not notify any change" in {
      val probe = TestProbe()

      // arrange the result file as expected
      TestUtils.changeFileInto(TestUtils.ACTUAL_DBPEDIA_RESULT_FILE_NAME, TestUtils.DBPEDIA_RESULT_FILE_NAME)
      TestUtils.changeFileInto(TestUtils.ACTUAL_IMDB_RESULT_FILE_NAME, TestUtils.IMDB_RESULT_FILE_NAME)
      // create a new actor
      val sqf = system.actorOf(Props(new MockSubQueryDistributor))
      probe watch sqf
      sqf ! fsq

      //create expected message instance
      val expectedResult = generateExpectedResult(fsq.hashCode)
      expectMsg(expectedResult)
      // send same query again
      sqf ! fsq
      // expect same result
      expectMsg(expectedResult)
      // kill actor instance
      sqf ! PoisonPill
      probe.expectTerminated(sqf)
    }
  }

  private def generateExpectedResult(expectedKey: Int) = {
    val imdbRes = ResultSetFactory.load(TestUtils.IMDB_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
    var bindings = generateBindings(imdbRes)
    val dbpediaRes = ResultSetFactory.load(TestUtils.DBPEDIA_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
    bindings ++= generateBindings(dbpediaRes)
    val expectedRes = constructResult(bindings, dbpediaRes.getResultVars, expectedKey)
    expectedRes
  }

  private def constructResult(bindingList: Vector[Binding], resVars: java.util.List[String], expectedKey: Int) = {
    val finalResultSet = ResultSetFactory.create(new QueryIterCollection(bindingList.asJava), resVars)
    val jsonText = MonitoringUtils.convertRdf2Json(finalResultSet)
    Result(jsonText, finalResultSet.getResultVars.asScala, expectedKey)
    //MonitoringUtils.convertRdf2Result(finalResultSet)
  }

  def generateBindings(resultSet: ResultSet) = {
    var bindingList: Vector[Binding] = Vector.empty
    while (resultSet.hasNext)
      bindingList = bindingList :+ resultSet.nextBinding
    bindingList
  }

}

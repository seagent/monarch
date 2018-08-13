import java.io.ByteArrayOutputStream

import actor.{MockSubQueryExecutor, MockSubQueryFederator}
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.hp.hpl.jena.query.{ResultSet, ResultSetFactory, ResultSetFormatter}
import com.hp.hpl.jena.sparql.engine.binding.Binding
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import main.QueryIterCollection
import monitoring.main.MonitoringUtils
import monitoring.message.{ExecuteSubQuery, FederateSubQuery, Result}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.collection.JavaConverters._

class SubQueryFederatorTest extends TestKit(ActorSystem("SubQueryFederatorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A SubQueryFederator actor" must {

    "federate a sub query and return result to its register list" in {
      val probe = TestProbe()
      // create a new actor
      val sqf = system.actorOf(Props(new MockSubQueryFederator))
      probe watch sqf
      // send execute sub query message
      val fsq = FederateSubQuery(DataSetCreator.PERSON_SELECT_QUERY, Vector(DataSetCreator.DBPEDIA_RESULT_FILE_NAME, DataSetCreator.IMDB_RESULT_FILE_NAME))
      sqf ! fsq

      //create expected message instance
      val dbpediaRes = ResultSetFactory.load(DataSetCreator.DBPEDIA_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      var bindings = generateBindings(dbpediaRes)
      val imdbRes = ResultSetFactory.load(DataSetCreator.IMDB_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      bindings ++= generateBindings(imdbRes)
      val expectedRes = constructResult(bindings, dbpediaRes.getResultVars)

      // check if received message is the expected one
      expectMsg(expectedRes)
      // kill actor instance
      sqf ! PoisonPill
      probe.expectTerminated(sqf)
    }
  }

  private def constructResult(bindingList: Vector[Binding], resVars: java.util.List[String]) = {
    val finalResultSet = ResultSetFactory.create(new QueryIterCollection(bindingList.asJava), resVars)
    val outputStream = new ByteArrayOutputStream
    ResultSetFormatter.outputAsJSON(outputStream, finalResultSet)
    val finalResult = Result(new String(outputStream.toByteArray), finalResultSet.getResultVars.asScala)
    finalResult
  }

  def generateBindings(resultSet: ResultSet) = {
    var bindingList: Vector[Binding] = Vector.empty
    while (resultSet.hasNext)
      bindingList = bindingList :+ resultSet.nextBinding
    bindingList
  }

}

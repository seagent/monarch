import actor.MockSubQueryExecutor
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.hp.hpl.jena.query.ResultSetFactory
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import monitoring.main.MonitoringUtils
import monitoring.message.ExecuteSubQuery
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.libs.json.Json

class SubQueryExecutorTest extends TestKit(ActorSystem("SubQueryExecutorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An Executor actor" must {

    "execute query and return result to its register list" in {
      val sqe = system.actorOf(Props(new MockSubQueryExecutor))
      sqe ! ExecuteSubQuery(DataSetCreator.DBPEDIA_DIRECTOR_SELECT_QUERY, DataSetCreator.RESULT_FILE_NAME)
      val rsExp = ResultSetFactory.load(DataSetCreator.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      expectMsg(MonitoringUtils.convertRdf2Result(rsExp))
      system.stop(sqe)
    }

  }

  // In order to handle result change, some query result should be stored locally and changed some data in it.
  "An Executor actor" must {

    val res = ResultSetFactory.load(DataSetCreator.ACTUAL_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
    val jsonText = MonitoringUtils.convertRdf2Json(res)
    val jsonValue=Json.parse(jsonText)

    "notify a change to its register list" in {
      val sqe = system.actorOf(Props(new MockSubQueryExecutor))
      sqe ! ExecuteSubQuery(DataSetCreator.DBPEDIA_DIRECTOR_SELECT_QUERY, DataSetCreator.RESULT_FILE_NAME)
      val rsExp = ResultSetFactory.load(DataSetCreator.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      expectMsg(MonitoringUtils.convertRdf2Result(rsExp))
      system.stop(sqe)
    }

  }

}

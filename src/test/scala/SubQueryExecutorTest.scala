import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.hp.hpl.jena.query.QueryExecutionFactory
import monitoring.actor.SubQueryExecutor
import monitoring.main.MonitoringUtils
import monitoring.message.{ExecuteSubQuery, ResultChange}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class SubQueryExecutorTest extends TestKit(ActorSystem("SubQueryExecutorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  private val query = "select * where {?movie <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Steven_Spielberg>. ?movie <http://xmlns.com/foaf/0.1/name> ?name}"

  private val endpoint = "http://dbpedia.org/sparql"

  "An Executor actor" must {

    "execute query and return result to its register list" in {
      val sqe = system.actorOf(Props(new SubQueryExecutor))
      sqe ! ExecuteSubQuery(query, endpoint)

      val exec = QueryExecutionFactory.sparqlService(endpoint, query)
      val result = MonitoringUtils.convertRdfToResult(exec.execSelect())
      expectMsg(result)
    }

  }

}

import java.io.{File, PrintWriter}

import DataSetCreator.RESULT_FILE_NAME
import actor.MockSubQueryExecutor
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.hp.hpl.jena.query.ResultSetFactory
import com.hp.hpl.jena.sparql.resultset.ResultsFormat
import monitoring.main.MonitoringUtils
import monitoring.message.{ExecuteSubQuery, ResultChange}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class SubQueryExecutorTest extends TestKit(ActorSystem("SubQueryExecutorTest")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An Executor actor" must {

    // arrange the result file as expected
    cleanUpResultFile

    "execute query and return result to its register list" in {
      // create a new actor
      val sqe = system.actorOf(Props(new MockSubQueryExecutor))
      // send execute sub query message
      sqe ! ExecuteSubQuery(DataSetCreator.DBPEDIA_DIRECTOR_SELECT_QUERY, DataSetCreator.RESULT_FILE_NAME)

      //create expected message instance
      val rsExp = ResultSetFactory.load(DataSetCreator.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      val expectedResult = MonitoringUtils.convertRdf2Result(rsExp)
      // check if received message is the expected one
      expectMsg(expectedResult)
      // tear down actor system
      system.stop(sqe)
    }

  }

  "An Executor actor" must {

    // arrange the result file as expected
    cleanUpResultFile

    // create changed result file by modifying actual result
    val changedJsonText = DataSetCreator.changeResultFile(DataSetCreator.ACTUAL_RESULT_FILE_NAME)

    "notify a change to its register list" in {
      // create a sub query executor actor
      val sqe = system.actorOf(Props(new MockSubQueryExecutor))
      // send an execute sub query message
      sqe ! ExecuteSubQuery(DataSetCreator.DBPEDIA_DIRECTOR_SELECT_QUERY, DataSetCreator.RESULT_FILE_NAME)
      // first expect an Result message
      val rsExp = ResultSetFactory.load(DataSetCreator.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      // assert expected message
      expectMsg(MonitoringUtils.convertRdf2Result(rsExp))
      // modify the registered result
      write2File(changedJsonText, RESULT_FILE_NAME)
      // create expected message instance
      val rsChanged = ResultSetFactory.load(DataSetCreator.RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
      val resultChangeMsgExp = ResultChange(MonitoringUtils.convertRdf2Result(rsChanged))
      // check if received result is result change message
      expectMsg(10.seconds, resultChangeMsgExp)
      // tear down the actor system
      system.stop(sqe)
    }

  }

  /**
    * This method re-arranges the result file to its original
    */
  private def cleanUpResultFile = {
    val res = ResultSetFactory.load(DataSetCreator.ACTUAL_RESULT_FILE_NAME, ResultsFormat.FMT_RS_JSON)
    val jsonText = MonitoringUtils.convertRdf2Json(res)
    //write original json text to file
    write2File(jsonText, RESULT_FILE_NAME)
  }

  /**
    * This method writes given @jsonText input to the location given in @filePath
    *
    * @param jsonText
    * @param filePath
    */
  private def write2File(jsonText: String, filePath: String) = {
    val pw = new PrintWriter(new File(filePath))
    pw.write(jsonText)
    pw.close()
  }
}

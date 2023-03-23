package actor

import java.util

import akka.actor.{ActorRef, Props}
import main.DirectedQuery
import monitoring.actor.Federator
import monitoring.message.{FederateSubQuery, Result}

import scala.collection.JavaConverters._

class MockQueryDistributor extends Federator {

  private val GEO_JOIN_RESULT_NAME = "src/test/files/geo-join.json"
  private val LMDB_JOIN_RESULT_NAME = "src/test/files/lmdb-join.json"
  private val DBPEDIA_JOIN_RESULT_NAME = "src/test/files/dbpedia-join-1.json"

  override protected def federate(query: String): Unit = {
    //val sqf = context.system.actorOf(Props(new MockSubQueryDistributor))
    federate(query, ActorRef.noSender)
  }

  override protected def federate(query: String, federator: ActorRef): Unit = {
    val directedQueries = new util.ArrayList[DirectedQuery]
    directedQueries.add(new DirectedQuery("query-1", util.Arrays.asList(LMDB_JOIN_RESULT_NAME)))
    directedQueries.add(new DirectedQuery("query-2", util.Arrays.asList(DBPEDIA_JOIN_RESULT_NAME)))
    directedQueries.add(new DirectedQuery("query-3", util.Arrays.asList(GEO_JOIN_RESULT_NAME)))
    distribute(federator, directedQueries)
  }

  override protected def directToSubQueryFederator(subQueryFederatorRegion: ActorRef, directedQuery: DirectedQuery): Unit = {
    val sqf = context.system.actorOf(Props(new MockSubQueryDistributor))
    sqf ! FederateSubQuery(directedQuery.getQuery, directedQuery.getEndpoints.asScala)
  }

  override protected def processResult(receivedResult: Result): Unit = {
    val bd = context.system.actorOf(Props(new MockParallelJoinManager))
    processResult(receivedResult)
  }

}

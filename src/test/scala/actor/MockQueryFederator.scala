package actor

import java.util
import java.util.List

import akka.actor.{ActorRef, Props}
import main.DirectedQuery
import monitoring.actor.QueryFederator

class MockQueryFederator extends QueryFederator {

  override protected def federate(query: String): Unit = {
    val sqf = context.system.actorOf(Props(new MockSubQueryFederator))
    federate(query, sqf)
  }

  override protected def federate(query: String, federator: ActorRef): Unit = {
    val directedQueries = new util.ArrayList[DirectedQuery]()
  }

}

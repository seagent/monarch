package actor

import monitoring.actor.SubQueryExecutor
import monitoring.message.Result

class MockSubQueryExecutor extends SubQueryExecutor {
  override protected def executeQuery(query: String, endpoint: String): Result = {
    Result("", Vector.empty[String])
  }
}

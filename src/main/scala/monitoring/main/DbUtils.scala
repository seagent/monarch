package monitoring.main

import monitoring.main.RedisStore.redisPool

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DbUtils {

  def increaseActorCount: Unit = {
    RedisStore.incr(Constants.ACTOR_COUNT)
  }

  def decreaseActorCount: Unit = {
    RedisStore.decr(Constants.ACTOR_COUNT)
  }

  def incrementQueryCount(anyQuery: Any) = {
    Future {
      val storedQuery = RedisStore.get(anyQuery.hashCode)
      if (storedQuery.isEmpty) {
        RedisStore.set(anyQuery.hashCode, anyQuery)
        RedisStore.incr(Constants.QUERY_COUNT)
      }
    }
  }

  def deleteStore = {
    RedisStore.deleteStore
  }

}

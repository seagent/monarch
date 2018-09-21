package monitoring.main

import com.redis.RedisClientPool

object RedisStore {

  val redisPool = new RedisClientPool("localhost", 6379)

  def set(key: Any, value: Any) = redisPool.withClient {
    client => {
      client.set(key, value)
    }
  }

  def incr(key: Any) = redisPool.withClient {
    client => {
      client.incr(key)
    }
  }

  def decr(key: Any) = redisPool.withClient {
    client => {
      client.decr(key)
    }
  }

  def get(key: Any) = redisPool.withClient {
    client => {
      client.get(key)
    }
  }

  def lpush(key: Any, value: Any) = redisPool.withClient {
    client => {
      client.lpush(key, value)
      client.llen(key)
    }
  }

  def rpush(key: Any, value: Any) = redisPool.withClient {
    client => {
      client.rpush(key, value)
      client.llen(key)
    }
  }

  def deleteStore = redisPool.withClient {
    client => {
      client.flushall
    }
  }

}

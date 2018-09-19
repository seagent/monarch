import com.redis._
import monitoring.main.RedisStore.redisPool
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


class RedisConnectionTest extends FlatSpec with Matchers {
  val clients = new RedisClientPool("localhost", 6379)

  "Redis client " should "do the operations to redis database" in {
    // delete store before start
    deleteStore
    set("my-key", "13")
    println(get("my-key").get)
    incr("my-key")
    println(get("my-key").get)
    val fut = Future {
      incr("my-key")
    }
    fut.onComplete {
      case Success(value) => {
        println(s"Key value: ${value.get}")
      }
      case Failure(e) => {
        println(s"Increment operation is failed because of: ${e.getMessage}")
      }
    }
    val res = Await.result(fut, 1.seconds).get
    println(res)

    rpush("my-list", "25")
    rpush("my-list", "26")
    rpush("my-list", "27")
    rpush("my-list", "25")
    //println(get("my-list"))
  }

  private def deleteStore = clients.withClient {
    client => {
      client.flushall
    }
  }

  def set(key: String, value: String) = clients.withClient {
    client => {
      client.set(key, value)
    }
  }

  def incr(key: String) = clients.withClient {
    client => {
      client.incr(key)
    }
  }

  def get(key: String) = clients.withClient {
    client => {
      client.get(key)
    }
  }

  def lpush(key: Any, value: Any) = clients.withClient {
    client => {
      client.lpush(key, value)
      client.llen(key)
    }
  }

  def rpush(key: Any, value: Any) = clients.withClient {
    client => {
      client.rpush(key, value)
      client.llen(key)
    }
  }

}

package monitoring

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import monitoring.actor.SubscriberAgent
import monitoring.message.Subscribe

object ClientApp {
    def main(args: Array[String]): Unit = {
      val config = ConfigFactory.parseString("akka.remote.artery.canonical.hostname = " + "127.0.0.1").
        withFallback(ConfigFactory.parseString("akka.remote.artery.canonical.port = " + 2553)).
        withFallback(ConfigFactory.load("agent.conf"))

      // Create an Akka system
      val system = ActorSystem("Subscribing", config)
      val subscriberAgent=system.actorOf(Props[SubscriberAgent])
      subscriberAgent!Subscribe("subscribe-query")
    }
}

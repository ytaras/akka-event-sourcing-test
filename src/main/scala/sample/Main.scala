package sample

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory

object Main extends App {
  val ports = Seq("2551", "2552", "0")
  ports foreach { port =>
    // Override the configuration of the port
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem("ClusterSystem", config)
    // Create an actor that handles cluster domain events
    system.actorOf(Props[SimpleClusterListener], name = "clusterListener")
    Cluster(system) registerOnMemberUp {
      val hello = system.actorOf(Props(classOf[HelloWorld]), name = "helloWorld")
      hello ! "port"
      hello ! port
    }
  }
}

class HelloWorld extends Actor with ActorLogging {

  def receive = {
    case x: String => log.info(s"Received {}", x)
    case Done => context.stop(self)
  }
}

class Terminator(actor: ActorRef) extends Actor with ActorLogging {
  context watch actor
  def receive = {
    case Terminated(_) =>
      log.info("Receved terminated")
      context.system.terminate()
  }
}

case object Done

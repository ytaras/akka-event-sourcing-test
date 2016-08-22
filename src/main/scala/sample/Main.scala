package sample

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.singleton._
import com.typesafe.config.ConfigFactory

object Main extends App {
  val ports = Seq("2551", "2552", "0")
  ports foreach { port =>
    // Override the configuration of the port
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem("ClusterSystem", config)

    val hello = system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[HelloWorld]),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)
      ), name = "hello"
    )
    val helloProxy = system.actorOf(
      ClusterSingletonProxy.props(singletonManagerPath = "/user/hello",
                                  settings = ClusterSingletonProxySettings(system)),
      name = "helloProxy")
    helloProxy ! s"Port: $port"
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

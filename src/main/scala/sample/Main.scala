package sample

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.singleton._
import akka.cluster.sharding._
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object Main extends App {
  val ports = Seq("2551", "2552", "0")
  ports foreach { port =>
    implicit val p = Port(port)
    // Override the configuration of the port
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())

    // Create an Akka system
    implicit val system = ActorSystem("ClusterSystem", config)

    val collector = system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[MessagesCollector]),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)
      ), name = "messagesCollector"
    )
    val collectorProxy = system.actorOf(
      ClusterSingletonProxy.props(singletonManagerPath = "/user/messagesCollector",
                                  settings = ClusterSingletonProxySettings(system)),
      name = "messagesCollectorProxy")

    val done: Future[_] = Generator.run.flatMap(_ => Enricher.run(collectorProxy)).flatMap { _ => system.terminate() }
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

case class Port(port: String)

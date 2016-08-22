package sample

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated }

object Main extends App {
  val system = ActorSystem()
  val hello = system.actorOf(Props(classOf[HelloWorld]), "hello-world")
  val terminator = system.actorOf(Props(classOf[Terminator], hello), "app-terminator")
  hello ! "Hello"
  hello ! "World"
  hello ! Done

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

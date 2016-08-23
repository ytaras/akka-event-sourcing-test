package sample

import akka.actor.{ Actor, ActorLogging }
import akka.kafka.ConsumerMessage.CommittableMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import scala.concurrent.duration._

class MessagesCollector extends Actor with ActorLogging {

  import context.dispatcher

  override def preStart = {
    self ! "print"
  }

  def receive = collect(List())
  def collect(state: List[(Long, String)]): Receive = {
    case (key: Long, value: String) =>
      context.become(collect((key, value) :: state))
    case "print" =>
      log.info("We have {} items saved", state.size)
      state.foreach{ case (k, v) => log.info("message saved {} -> {}", k, v) }
      context.system.scheduler.scheduleOnce(5.seconds, self, "print")
  }

}

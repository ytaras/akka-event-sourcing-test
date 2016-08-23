package sample

import akka.NotUsed
import akka.actor.{ ActorRef, ActorSystem, PoisonPill }
import akka.event.Logging
import akka.kafka.ConsumerMessage.{ CommittableMessage, CommittableOffsetBatch }
import akka.kafka.scaladsl._
import akka.kafka._
import akka.stream.ActorMaterializer
import akka.stream.Attributes
import akka.stream.scaladsl._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ LongDeserializer, LongSerializer, StringDeserializer, StringSerializer }
import java.lang.{Long => JLong}
import scala.concurrent.Future

object Enricher {
  def run(sendTo: ActorRef)(implicit system: ActorSystem, port: Port): Future[_] = {
    implicit val materializer = ActorMaterializer()
    val consumerSettings = ConsumerSettings(system, new LongDeserializer(), new StringDeserializer())
      .withBootstrapServers("localhost:9092")
      .withGroupId("enricher")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val source = Consumer
      .committableSource(consumerSettings, Subscriptions.topics("rawEvents"))
      .log("Reveived")
      .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))

    val sendToActor: Sink[CommittableMessage[JLong, String], NotUsed] =
      Flow[CommittableMessage[JLong, String]]
        .map { x =>  x.record.key -> s"""Message "${x.record.value}" received in $port""" }
        .to(Sink.actorRef(sendTo, Done))

    val commit: Sink[CommittableMessage[JLong, String], Future[_]] =
      Flow[CommittableMessage[JLong, String]]
        .map { _.committableOffset }
        .batch(max = 20, CommittableOffsetBatch.empty.updated(_)) { _.updated(_) }
        .log("Before commit")
        .withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
        .mapAsync(3)(_.commitScaladsl())
        .toMat(Sink.ignore)(Keep.right)

    source
      .alsoTo(sendToActor)
      .runWith(commit)
  }

}

object Generator {
  def run(implicit system: ActorSystem, port: Port): Future[_] = {
    implicit val materializer = ActorMaterializer()

    val producerSettings = ProducerSettings(system, new LongSerializer(), new StringSerializer).withBootstrapServers("10.128.176.171:9092")
    Source(0 to 10)
      .map { i =>
      ProducerMessage.Message(
        new ProducerRecord[JLong, String]("rawEvents", i, s"new - Mesage for key $i from $port"), i
      )
    }
      .via(Producer.flow(producerSettings))
      .runWith(Sink.ignore)
  }
}

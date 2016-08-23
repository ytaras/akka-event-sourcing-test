package sample

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
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
  def run(implicit system: ActorSystem): Future[_] = {
    implicit val materializer = ActorMaterializer()
    val consumerSettings = ConsumerSettings(system, new LongDeserializer(), new StringDeserializer())
      .withBootstrapServers("localhost:9092")
      .withGroupId("enricher")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    Consumer
      .committableSource(consumerSettings, Subscriptions.topics("rawEvents"))
      .log("Reveived")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .map { msg => msg.committableOffset }
      .batch(max = 1000, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) =>
        batch.updated(elem)
      }
      .log("Before commit")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .mapAsync(3)(_.commitScaladsl())
      .runWith(Sink.ignore)
  }

}

object Generator {
  def run(implicit system: ActorSystem): Future[_] = {
    implicit val materializer = ActorMaterializer()

    val producerSettings = ProducerSettings(system, new LongSerializer(), new StringSerializer).withBootstrapServers("10.128.176.171:9092")
    Source(0 to 10)
      .map { i =>
      ProducerMessage.Message(
        new ProducerRecord[JLong, String]("rawEvents", i, s"new - Mesage for key $i"), i
      )
    }
      .via(Producer.flow(producerSettings))
      .runWith(Sink.ignore)
  }
}

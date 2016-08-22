package sample

import akka.actor.ActorSystem
import akka.kafka.scaladsl._
import akka.kafka._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ LongDeserializer, LongSerializer, StringDeserializer, StringSerializer }
import java.lang.{Long => JLong}

class Enricher(implicit system: ActorSystem) {
  implicit val materializer = ActorMaterializer()

  val consumerSettings = ConsumerSettings(system, new LongDeserializer(), new StringDeserializer())
    .withBootstrapServers("localhost:9092")

  Consumer
    .committableSource(consumerSettings, Subscriptions.topics("rawEvents"))
    .runForeach { msg =>
    println(
      s"{msg.record.key()} - ${msg.record.value()} - $msg"
    )
  }

}

class Generator(implicit system: ActorSystem) {
  implicit val materializer = ActorMaterializer()

  val producerSettings = ProducerSettings(system, new LongSerializer(), new StringSerializer).withBootstrapServers("10.128.176.171:9092")

  Source(0 to 100000)
    .map { i =>
    ProducerMessage.Message(
      new ProducerRecord[JLong, String]("rawEvents", i, s"Mesage for key $i"), i
    )
  }
    .alsoTo(Sink.foreach(println))
    .via(Producer.flow(producerSettings))
    .runWith(Sink.foreach(println))
}

package com.example

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.logRegistry.kafkaProps
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scala.collection.JavaConverters._

// This class basically gets set up in the beginning (check main) and starts polling the kafka topic for changes
// till the application terminates. Also when we do get messages in our topic, we put the extracted forms of this
// message into another topic for the Spark program to aggregate and send to stakeholders

object kafkaConsumerActor {

  def apply(): Behavior[FileName] = {
    // We have this running from the beginning
    Behaviors.setup { context =>
        // Setup helpers
        val logger = CreateLogger(this.getClass)
        logger.info("reached kafka consumer")
        val consumer = new KafkaConsumer[String,String](kafkaProps)
        val topics = List("logMessages")

        try {
          consumer.subscribe(topics.asJava)
          while (true) {
            val records = consumer.poll(100)
            val scalaRecords: Iterable[ConsumerRecord[String, String]] = records.asScala
            val justValues = scalaRecords.map((x: ConsumerRecord[String, String]) => x.value())
            val allRecords = justValues.groupBy((line: String) => {
              if (line.contains("WARN")) "WARN"
              else "ERROR"
            })

            val producer = new KafkaProducer[String, String](kafkaProps)
            allRecords.foreach((line: (String, Iterable[String])) => {
              line._2.foreach(produceRecordString =>{
                val produceRecord = new ProducerRecord[String, String]("extractedMessages", line._1, produceRecordString)
                producer.send(produceRecord)
              }
              )
            })
          }
        }catch{
          case e:Exception => e.printStackTrace()
        }finally {
          consumer.close()
        }

      Behaviors.stopped
    }
  }
}

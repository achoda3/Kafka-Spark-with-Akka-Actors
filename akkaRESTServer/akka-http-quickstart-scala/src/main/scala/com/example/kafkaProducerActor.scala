package com.example

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.logRegistry.kafkaProps
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.io.Source

// This class reads the file I get from the s3 Bucket and sends all the messages with WARN and ERROR to the kafka topic
// that the cosumer is polling for

object kafkaProducerActor {
  def apply(): Behavior[FileName] =
  Behaviors.receiveMessage {
    case FileName(filename) =>

      val config = ConfigFactory.load()
      val Lines = Source.fromFile(filename).getLines().toList
      val logger = CreateLogger(this.getClass)
      logger.info("reached kafka")
      val producer = new KafkaProducer[String, String](kafkaProps)
      val printAll = Lines.map(line => logger.info(line))
      val all = Lines.map(line => line.split(" "))
      val timeandType = all.map(line => Array[String](line(0),line(2)))
      val time = all.map(line => line(0))
      //println(time)
      val second = time.map(x=>x.substring(0,8))
      val distincIntervals = second.distinct
      val interval = timeandType.flatMap(message => distincIntervals.map(interval => if(interval==message(0).substring(0,8)) interval+message(1) else "null"))
      val nonNull = interval.filter(x => x!="null")
      val warns = nonNull.filter(time => if(time.substring(8,12)=="WARN")  true else false)
      try {
        val sendFiles = warns.map(line => {
          val produceRecord = new ProducerRecord[String, String]("logMessages", filename, line)
          producer.send(produceRecord)
        })
      } catch{
        case e:Exception => e.printStackTrace()
      }finally {
        producer.close()
      }
      Behaviors.same

  }
}

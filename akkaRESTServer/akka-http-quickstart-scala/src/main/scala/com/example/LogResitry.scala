package com.example

//#user-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringSerializer
import org.codehaus.jackson.map.deser.std.StringDeserializer

import java.io.File
import java.util.Properties
//import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
//import com.amazonaws.services.s3.AmazonS3ClientBuilder

import scala.collection.immutable

//#user-case-classes
final case class Log(bucket: String, key: String)
final case class FileName(filename: String)

//#user-case-classes
// This actor basically sends the messages to all my other actors, has the definitions for all my messages and just
// acts as kind of a hub for handling my messages

object logRegistry {
  // Properties for the kafka Actors
  def kafkaProps: Properties = {
    val config = ConfigFactory.load()
    val props = new Properties()
    props.put("group.id", "kafka-app")
    props.put("bootstrap.servers", config.getString("my-app.boot-strap"))
    props.put("key.serializer", classOf[StringSerializer].getName)
    props.put("value.serializer", classOf[StringSerializer].getName)
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("acks","all")
    props
  }
  // actor protocol
  sealed trait Command
  final case class GetLog(log: Log, producer: ActorRef[FileName], consumer: ActorRef[FileName], replyTo: ActorRef[ActionPerformed]) extends Command
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry()

  private def registry(): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetLog(log, producer, consumer, replyTo) =>
        // Comment this out for last test case if not in EC2
        // Connect to S3
        val s3Client = AmazonS3ClientBuilder.standard()
          .withCredentials(new EnvironmentVariableCredentialsProvider())
          .build()
        val newFile = new File(log.key)
        val fileRead = s3Client.getObject(new GetObjectRequest(log.bucket,log.key), newFile)
        // Give time for file to be created before spawning next actor
        Thread.sleep(10000)

        val logger = CreateLogger(this.getClass)
        logger.info("reached Registry")
        logger.info(log.key)

        // Producer gets sent the file name that got created
        producer ! FileName(log.key)
        // We reply back to Router that we read the file
        replyTo ! ActionPerformed("file read")

        Behaviors.same

    }
}
//#user-registry-actor

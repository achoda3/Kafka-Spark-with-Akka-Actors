package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory

import scala.util.Failure
import scala.util.Success
// This is my main for the Server Application. It spawns all my actors and gets the server running
// I create the actor system and basically jump start the application here (main)
//#main-class
object QuickstartApp {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext
    val config = ConfigFactory.load()
    val futureBinding = Http().newServerAt(config.getString("my-app.IP"), config.getString("my-app.PORT").toInt).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
  // main starts the server
  //#start-http-server
  def main(args: Array[String]): Unit = {
    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val logRegistryActor = context.spawn(logRegistry(), "logRegistryActor")
      context.watch(logRegistryActor)

      val kafkaProducer = context.spawn(kafkaProducerActor(), name = "kafkaActor")
      context.watch(kafkaProducer)

      val kafkaConsumer = context.spawn(kafkaConsumerActor(), name = "kafkaConsumer")
      context.watch(kafkaConsumer)

      val routes = new logRoutes(logRegistryActor)(kafkaProducer)(kafkaConsumer)(context.system)
      startHttpServer(routes.logRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class

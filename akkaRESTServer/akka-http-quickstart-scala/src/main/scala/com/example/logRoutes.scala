package com.example

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{delete, get, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.example.logRegistry.{ActionPerformed, GetLog}

import scala.concurrent.Future
//#import-json-formats
//#user-routes-class
// This class basically handles all the routing of requests POST, GET, etc.
class logRoutes(theLogRegistry: ActorRef[logRegistry.Command])(theKafkaActor: ActorRef[FileName])(theKafkaConsumer: ActorRef[FileName])(implicit val system: ActorSystem[_]) {

  //#user-routes-class
  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))
  implicit val executionContext = system.executionContext
  //#all-routes

  // I define my function from my registry to return a future and pass the ActorRef of this class too
  def getLog(log: Log): Future[ActionPerformed] = {
    theLogRegistry.ask(GetLog(log, theKafkaActor, theKafkaConsumer, _))
  }

  //#users-get-post
  //#users-get-delete
  // all the Routing
  val logRoutes: Route = {
    concat(
      post {
        // Takes the json passed in and converts it to the custom defined type called Log (in Registry)
        entity(as[Log]) { log =>
          val logger = CreateLogger(this.getClass)
          logger.info("reached post")
          onSuccess(getLog(log)) { performed =>
            complete((StatusCodes.Created, performed))
          }
        }
      },
        get {
        complete("Please send POST")
      },
        delete {
        complete("Please send POST")
      }
    )

  }
}

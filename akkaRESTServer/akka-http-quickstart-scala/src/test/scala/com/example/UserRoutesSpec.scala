package com.example

//#user-routes-spec
//#test-top
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

//#set-up
class UserRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  //#test-top

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // Here we need to implement all the abstract members of UserRoutes.
  // We use the real UserRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe
  // created with testKit.createTestProbe()
  val kafkaProducer = testKit.spawn(kafkaProducerActor(), name = "kafkaActor")

  val kafkaConsumer = testKit.spawn(kafkaConsumerActor(), name = "kafkaConsumer")

  val theLogRegistry = testKit.spawn(logRegistry())
  lazy val routes = new logRoutes(theLogRegistry)(kafkaProducer)(kafkaConsumer).logRoutes

  // use the json formats to marshal and unmarshall objects in the test
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#set-up

  //#actual-test
  "UserRoutes" should {
    "return nothing not POST - GET" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/log")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // and no entries should be in the list:
        entityAs[String] should ===("Please send POST")
      }
    }
    //#actual-test

    //#testing-delete
    "return nothing not POST - Delete" in {
      // note that there's no need for the host part in the uri:
      val request = Delete(uri = "/log")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // and no entries should be in the list:
        entityAs[String] should ===("Please send POST")
      }
    }

    //# testing GET
    "return Nothing if JSON in GET" in {
      val user = Log("bucket", "input.log")
      val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Get("/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // and no entries should be in the list:
        entityAs[String] should ===("Please send POST")
      }
    }

    //# testing DELETE
    "return Nothing if JSON in DELETE" in {
      val config = ConfigFactory.load()
      val user = Log(config.getString("my-app.BUCKET_NAME"), config.getString("my-app.FILE_NAME"))
      val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Delete("/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // and no entries should be in the list:
        entityAs[String] should ===("Please send POST")
      }
    }

    //# testing POST [Comment out amazon connection and message to producer to Test]
    "return File Read if JSON in POST" in {
      val config = ConfigFactory.load()
      val user = Log(config.getString("my-app.BUCKET_NAME"), config.getString("my-app.FILE_NAME"))
      val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"file read"}""")
      }
    }

  }
  //#actual-test

  //#set-up
}
//#set-up
//#user-routes-spec

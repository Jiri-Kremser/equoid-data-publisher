package io.radanalytics.equoid

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RestTest extends Specification with Specs2RouteTest with PublisherService {
  def actorRefFactory = system

  "PublisherService" should {

    "return a basic description for GET requests to the /api path" in {
      Get("/api") ~> myRoute ~> check {
        responseAs[String] must contain("data-publisher")
      }
    }

    "return a add the item and return it for POST requests to the /api path" in {
      Post("/api", "new-item") ~> myRoute ~> check {
        status === Created
        responseAs[String] == "new-item"
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/unknown") ~> myRoute ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the /api path" in {
      Put("/api") ~> sealRoute(myRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET, POST"
      }
    }
  }
}
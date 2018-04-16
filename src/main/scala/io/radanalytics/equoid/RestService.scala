package io.radanalytics.equoid

import akka.actor.Actor
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http._
import spray.routing._

class PublisherServiceActor extends Actor with PublisherService {

  def actorRefFactory = context
  def receive = runRoute(myRoute)
}

trait PublisherService extends HttpService {

  val myRoute =
    path("api") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                This is data-publisher micro service. Use the HTTP POST request to add new frequent item.
              </body>
            </html>
          }
        }
      } ~
        post {
          respondWithStatus(Created) {
            entity(as[String]) { item =>
              DataPublisher.addFrequentItem(item)
              complete(item)
            }
          }
        }
    }
}
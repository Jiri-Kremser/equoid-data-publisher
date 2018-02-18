package io.radanalytics.equoid 

import java.lang.Long

import io.vertx.core.{AsyncResult, Handler, Vertx}
import io.vertx.proton._
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.message.Message

import scala.util.Random
import scala.io.Source

/**
  * Sample application which publishes records to an AMQP node
  */
object dataPublisher {

  private var host: String = "broker-amq-amqp"
  private var port: Int = 5672
  private var username: String = "daikon"
  private var password: String = "daikon"
  private var address: String = "salesq"
  private var dataURL: String = "https://raw.githubusercontent.com/EldritchJS/equoid-data-publisher/master/data/LiquorNames.txt"

  def main(args: Array[String]): Unit = {

    if (args.length < 6) {
      System.err.println("Usage: dataPublisher <hostname> <port> <username> <password> <queue> <data URL>")
      System.exit(1)
    }

    host = args(0)
    port = args(1).toInt
    username = args(2)
    password = args(3)
    address = args(4)
    dataURL = args(5)
    val vertx: Vertx = Vertx.vertx()

    val client:ProtonClient = ProtonClient.create(vertx)
    val opts:ProtonClientOptions = new ProtonClientOptions()
    opts.setReconnectAttempts(13)
    client.connect(opts, host, port, username, password, new Handler[AsyncResult[ProtonConnection]] {
      override def handle(ar: AsyncResult[ProtonConnection]): Unit = {
        if (ar.succeeded()) {

          val connection: ProtonConnection = ar.result()
          connection.open()

          val sender: ProtonSender = connection.createSender(address)
          sender.open()

          val random = new Random()
          val fileiter = Source.fromURL(dataURL)
          val buffer = fileiter.getLines()
          buffer.drop(1)
          val total = Source.fromURL(dataURL).getLines.size
          vertx.setPeriodic(1000, new Handler[Long] {
            override def handle(timer: Long): Unit = {

              val index: Int = random.nextInt(total)
              val message: Message = ProtonHelper.message()
              val record = if(buffer.hasNext) buffer.next() else fileiter.reset()
              message.setBody(new AmqpValue(record)) 

              println("Record = " + record)
              println("Message = " + message)
              sender.send(message, new Handler[ProtonDelivery] {
                override def handle(delivery: ProtonDelivery): Unit = {

                }
              })
            }
          })

        } else {
          println("Async connect attempt failed")
        }
      }
    })
  }
}

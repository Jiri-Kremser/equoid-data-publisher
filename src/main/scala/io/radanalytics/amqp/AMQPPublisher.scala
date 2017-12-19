package io.radanalytics.amqp.publisher 

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
object AMQPPublisher {

  private var host: String = "broker-amq-amqp"
  private var port: Int = 5672
  private var username: String = "daikon"
  private var password: String = "daikon"
  private var address: String = "salesq"
  private var datafile: String = "LiquorNames.txt"

  def main(args: Array[String]): Unit = {

    if (args.length < 6) {
      System.err.println("Usage: AMQPPublisher <hostname> <port> <username> <password> <queue> <datafilename>")
      System.exit(1)
    }

    host = args(0)
    port = args(1).toInt
    username = args(2)
    password = args(3)
    address = args(4)
    datafile = args(5)
    val vertx: Vertx = Vertx.vertx()

    val client:ProtonClient = ProtonClient.create(vertx)

    client.connect(host, port, username, password, new Handler[AsyncResult[ProtonConnection]] {
      override def handle(ar: AsyncResult[ProtonConnection]): Unit = {
        if (ar.succeeded()) {

          val connection: ProtonConnection = ar.result()
          connection.open()

          val sender: ProtonSender = connection.createSender(address)
          sender.open()

          val random = new Random()
          val fileiter = Source.fromFile(datafile)
          val buffer = fileiter.getLines()
          buffer.drop(1)
          val total = Source.fromFile(datafile).getLines.size
          vertx.setPeriodic(1000, new Handler[Long] {
            override def handle(timer: Long): Unit = {

              val index: Int = random.nextInt(total)
              val message: Message = ProtonHelper.message()
              val record = if(buffer.hasNext) buffer.next() else fileiter.reset()
              message.setBody(new AmqpValue(record)) 

              println("Record = " + record)
              sender.send(message, new Handler[ProtonDelivery] {
                override def handle(delivery: ProtonDelivery): Unit = {

                }
              })
            }
          })

        }
      }
    })

    System.in.read()
  }
}

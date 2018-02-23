package io.radanalytics.equoid 

import java.lang.Long

import io.vertx.core.{AsyncResult, Handler, Vertx}
import io.vertx.proton._
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.message.Message

import scala.util.Properties
import scala.util.Random
import scala.io.Source

/**
  * Sample application which publishes records to an AMQP node
  */
object dataPublisher {

  def getProp(camelCaseName: String, defaultValue: String): String = {
    val snakeCaseName = camelCaseName.replaceAll("(.)(\\p{Upper})", "$1_$2").toUpperCase()
    Properties.envOrElse(snakeCaseName, Properties.scalaPropOrElse(snakeCaseName, defaultValue))
  }

  def main(args: Array[String]): Unit = {

    val host = getProp("amqpHost", "broker-amq-amqp")
    val port = getProp("amqpPort", "5672").toInt
    val username = getProp("amqpUsername", "daikon")
    val password = getProp("amqpPassword", "daikon")
    val address = getProp("queueName", "salesq")
    val dataURL = getProp("dataUrl", "https://raw.githubusercontent.com/EldritchJS/equoid-data-publisher/master/data/LiquorNames.txt")
    val vertx: Vertx = Vertx.vertx()

    val client:ProtonClient = ProtonClient.create(vertx)
    val opts:ProtonClientOptions = new ProtonClientOptions()
    opts.setReconnectAttempts(1)
        .setTrustAll(true)
        .setConnectTimeout(10000) // timeout = 10sec, reconnect interval is 1sec
    client.connect(opts, host, port, username, password, new Handler[AsyncResult[ProtonConnection]] {
      override def handle(ar: AsyncResult[ProtonConnection]): Unit = {
        if (ar.succeeded()) {

          val connection: ProtonConnection = ar.result()
          connection.open()

          val sender: ProtonSender = connection.createSender(address)
          sender.open()
          println(s"Connection to $host:$port has been successfully established")

          val fileIter = Source.fromURL(dataURL)
          val buffer = fileIter.getLines()
          buffer.drop(1)
          vertx.setPeriodic(1000, new Handler[Long] {
            override def handle(timer: Long): Unit = {
              val message: Message = ProtonHelper.message()
              val record = if(buffer.hasNext) buffer.next() else fileIter.reset()
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
          println(s"Async connect attempt to $host:$port failed")
          println(s"Cause: ${ar.cause().getMessage}")
          ar.cause().printStackTrace()
        }
      }
    })
  }
}

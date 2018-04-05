package io.radanalytics.equoid 

import java.lang.Long

import io.vertx.core.{AsyncResult, Handler, Vertx}
import io.vertx.proton._
import org.apache.commons.math3.distribution.ZipfDistribution
import org.apache.commons.math3.random.Well512a
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.message.Message

import scala.util.Properties
import scala.io.{Codec, Source}

/**
  * Sample application which publishes records to an AMQP node
  */
object DataPublisher {

  def getProp(snakeCaseName: String, defaultValue: String): String = {
    // return the value of 'SNAKE_CASE_NAME' env variable,
    // if ^ not defined, return the value of 'snakeCaseName' JVM property
    // if ^ not defined, return the defaultValue
    val camelCase = "_(.)".r.replaceAllIn(snakeCaseName.toLowerCase, m => m.group(1).toUpperCase)
    Properties.envOrElse(snakeCaseName, Properties.propOrElse(camelCase, defaultValue))
  }

  def main(args: Array[String]): Unit = {

    val host = getProp("AMQP_HOST", "broker-amq-amqp")
    val port = getProp("AMQP_PORT", "5672").toInt
    val username = getProp("AMQP_USERNAME", "daikon")
    val password = getProp("AMQP_PASSWORD", "daikon")
    val address = getProp("QUEUE_NAME", "salesq")
    val dataURL = getProp("DATA_URL", "https://raw.githubusercontent.com/EldritchJS/equoid-data-publisher/master/data/LiquorNames.txt")
    val vertx: Vertx = Vertx.vertx()
    val client:ProtonClient = ProtonClient.create(vertx)
    val opts:ProtonClientOptions = new ProtonClientOptions()
    opts.setReconnectAttempts(20)
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

          val zipfianIterator = ZipfianPicker[String](dataURL)
          vertx.setPeriodic(1000, new Handler[Long] {
            override def handle(timer: Long): Unit = {
              val message: Message = ProtonHelper.message()
              val record = if(zipfianIterator.hasNext) zipfianIterator.next else {
                zipfianIterator.reset()
                zipfianIterator.next
              }
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

  class ZipfianPicker[T](val filePath: String, val lines: Vector[T], var indexIterator: Iterator[Int], val seed: Int) extends Iterator[T] {

    override def foreach[U](f: T => U): Unit = {
      while (indexIterator.hasNext) f(lines(indexIterator.next()))
    }

    override def hasNext: Boolean = indexIterator.hasNext

    override def next: T = lines(indexIterator.next)

    def reset(seed: Int = this.seed): Unit = this.indexIterator = newIndexIterator(lines.length, seed)

    def newIndexIterator(length: Int, seed: Int): Iterator[Int] = {
      val zipf = new ZipfDistribution(new Well512a(seed), length, 1)
      // this is pretty slow
      Vector.fill(length) { zipf.sample() - 1 }.iterator
    }

    // if we ever need full blown for-comprehension w/ 'yield' and everything, we can also implement following methods
    //    def map[B](f: T => B): ZipfianCherryPicker[B] = ???
    //    def flatMap[B](f: T => ZipfianCherryPicker[B]): ZipfianCherryPicker[B] = ???
  }

  object ZipfianPicker extends ZipfianPicker("", null, null, 0) {

    def apply[T](filePath: String, seed: Int = 1313): ZipfianPicker[T] = {
      println(s"Initializing file $filePath ...")
      val fileIter = Source.fromURL(filePath)(Codec.UTF8)
      val lines: Vector[T] = fileIter.getLines().map(_.asInstanceOf[T]).toVector
      val indexIterator = newIndexIterator(lines.length, seed)
      new ZipfianPicker[T](filePath, lines, indexIterator, seed)
    }
  }


}

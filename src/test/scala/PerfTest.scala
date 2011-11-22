package com.banno.akka.event.flume
import akka.actor.Actor
import akka.event.EventHandler

object PerfTest {
  def main(args: Array[String]) {
    val listener = Actor.actorOf(new FlumeSinkEventHandlerListenerPool(() => FlumeSinkEventHandlerListener.sinkFor("{ batch(100) => { gzip => rpcSink(\"localhost\",12345)}}"))).start
    EventHandler.addListener(listener)

    Thread.sleep(10000L)
    System.err.println("Starting...")
    (1 to 50).foreach { i =>
      Actor.spawn {
        (1 to 100000).foreach { _ =>
          EventHandler.info(this, "hello from " + i)
        }
      }
    }
  }
}

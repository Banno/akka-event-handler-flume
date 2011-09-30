package com.banno.akka.event.flume
import com.cloudera.flume.handlers.debug.MemorySinkSource
import com.cloudera.flume.reporter.aggregator.CounterSink
import akka.event.EventHandler
import org.specs2.mutable.Specification

object FlumeSinkEventHandlerListenerSpec extends Specification {
  "Event Handler with a flume sink" should {
    "send events to a sink" in {
      val sink = new MemorySinkSource
      FlumeSinkEventHandlerListener.addListenerSink(sink)
      EventHandler.info(this, "hello")
      Option(sink.next) must eventually (beSome)
    }
  }
}

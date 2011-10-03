package com.banno.akka.event.flume
import com.cloudera.flume.core.Event
import com.cloudera.flume.handlers.debug.MemorySinkSource
import com.cloudera.flume.reporter.aggregator.CounterSink
import akka.event.EventHandler
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

object FlumeSinkEventHandlerListenerSpec extends Specification {
  "Event Handler with a flume sink" should {
    "send events to a sink" in new sinkSource {
      FlumeSinkEventHandlerListener.addListenerSink(sink)
      EventHandler.info(this, "hello")
      
      val nextEvent = getNextEvent(sink)
      nextEvent() must eventually (beSome)
      
      val event = nextEvent().get
      new String(event.get("sender")) must_== "com.banno.akka.event.flume.FlumeSinkEventHandlerListenerSpec$$anonfun$1$$anonfun$apply$1$$anon$1"
      event.getPriority must_== Event.Priority.INFO
    }
  }

  trait sinkSource extends Scope {
    lazy val sink = new MemorySinkSource
    
    def getNextEvent(sink: MemorySinkSource): () => Option[Event] = {
      var nextEvent: Option[Event] = None
      () => nextEvent orElse Option(sink.next) map { ev =>
        nextEvent = Some(ev)
        ev
      }
    }
  
  }
}

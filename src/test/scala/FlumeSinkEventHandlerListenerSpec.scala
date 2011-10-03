package com.banno.akka.event.flume
import com.cloudera.flume.core.Event
import com.cloudera.flume.handlers.debug.MemorySinkSource
import com.cloudera.flume.reporter.aggregator.CounterSink
import akka.event.EventHandler
import org.specs2.mutable.{After, Specification}
import org.specs2.specification.Scope

object FlumeSinkEventHandlerListenerSpec extends Specification {
  sequential
  
  "Event Handler with a flume sink" should {
    "send events to a sink" in new sinkSource {
      EventHandler.addListener(listener)
      EventHandler.info(this, "hello")
      
      val nextEvent = getNextEvent(sink)
      nextEvent() must eventually (beSome)
      
      val event = nextEvent().get
      new String(event.get("sender")) must_== "com.banno.akka.event.flume.FlumeSinkEventHandlerListenerSpec$$anonfun$1$$anonfun$apply$1$$anon$1"
      event.getPriority must_== Event.Priority.INFO
    }

    "add stacktraces for error events" in new sinkSource {
      EventHandler.addListener(listener)
      EventHandler.error(new RuntimeException("ouch"), this," hello")
      
      val nextEvent = getNextEvent(sink)
      nextEvent() must eventually (beSome)

      val event = nextEvent().get
      event.getPriority must_== Event.Priority.ERROR
      new String(event.get("exceptionType")) must_== "java.lang.RuntimeException"
      new String(event.get("exceptionMessage")) must_== "ouch"
      new String(event.get("exceptionBacktrace")) must contain("at com.banno.akka.event.flume.FlumeSinkEventHandlerListenerSpec$$anonfun$1$$anonfun$apply$4$$anon$2$delayedInit$body.apply(FlumeSinkEventHandlerListenerSpec.scala:27)")
    }
  }

  trait sinkSource extends Scope with After {
    lazy val sink = new MemorySinkSource
    lazy val listener = FlumeSinkEventHandlerListener.listenerFor(sink)
    
    def getNextEvent(sink: MemorySinkSource): () => Option[Event] = {
      var nextEvent: Option[Event] = None
      () => nextEvent orElse Option(sink.next) map { ev =>
        nextEvent = Some(ev)
        ev
      }
    }

    def after = EventHandler.removeListener(listener)
  
  }
}

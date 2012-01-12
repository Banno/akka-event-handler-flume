package com.banno.akka.event.flume
import com.cloudera.flume.core.Event
import com.cloudera.flume.handlers.debug.{MemorySinkSource, StubbornAppendSink}
import com.cloudera.flume.handlers.thrift.ThriftEventSink
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
      new String(event.get("sender")) must contain("FlumeSinkEventHandlerListenerSpec$")
      event.getPriority must_== Event.Priority.INFO
      new String(event.get("threadName")) must not (beEmpty)
      event.getTimestamp must beGreaterThan(0L)
      new String(event.getBody) must_== "hello"
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
      new String(event.get("exceptionBacktrace")) must contain("FlumeSinkEventHandlerListenerSpec.scala")
    }

    trait Animal { def numberOfLegs: Int }
    case class Cat(name: String) extends Animal { val numberOfLegs = 4 }
    
    "register a decorator for a given class" in new sinkSource {
      FlumeEventDecorators.decorate { (c: Cat, ev: FlumeEvent) =>
        ev("catName") = c.name
      }
       
      EventHandler.addListener(listener)
      EventHandler.info(this, Cat("lou"))
      
      val nextEvent = getNextEvent(sink)
      nextEvent() must eventually (beSome)

      val event = nextEvent().get
      event.getPriority must_== Event.Priority.INFO
      new String(event.get("catName")) must_== "lou"
    }

    "register a decorator for a super class" in new sinkSource {
      FlumeEventDecorators.decorate { (animal: Animal, ev: FlumeEvent) =>
        ev("numberOfLegs") = animal.numberOfLegs.toString
      }
       
      EventHandler.addListener(listener)
      EventHandler.info(this, Cat("lou"))
      
      val nextEvent = getNextEvent(sink)
      nextEvent() must eventually (beSome)

      val event = nextEvent().get
      event.getPriority must_== Event.Priority.INFO
      new String(event.get("numberOfLegs")) must_== "4"
    }

    "register a multiple decorators for a class" in new sinkSource {
      FlumeEventDecorators.decorate { (_: Animal, ev: FlumeEvent) =>
        ev("world") = "Earth"
      }
      
      FlumeEventDecorators.decorate { (_: Cat, ev: FlumeEvent) =>
        ev("type") = "Cat"
      }
      
      FlumeEventDecorators.decorate { (_: Cat, ev: FlumeEvent) =>
        ev("sound") = "meow"
      }
      
      EventHandler.addListener(listener)
      EventHandler.info(this, Cat("lou"))
      
      val nextEvent = getNextEvent(sink)
      nextEvent() must eventually (beSome)

      val event = nextEvent().get
      event.getPriority must_== Event.Priority.INFO
      new String(event.get("world")) must_== "Earth"
      new String(event.get("type")) must_== "Cat"
      new String(event.get("sound")) must_== "meow"
    }

    "allow disk failover" in new diskFailoverDecoSink {
      EventHandler.info(this, "ouch") must not (throwA[Exception])
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

    def after = {
      EventHandler.removeListener(listener)
      FlumeEventDecorators.clear
    }
  }

  trait diskFailoverDecoSink extends sinkSource {
    override lazy val listener = FlumeSinkEventHandlerListener.listenerFor("diskFailover stubbornAppend rpcSink(\"localhost\", 35869)")
  }
}

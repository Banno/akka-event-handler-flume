package com.banno.akka.event.flume
import akka.actor.ActorSystem
import akka.event.{BusLogging, Logging, LogSource}
import com.typesafe.config.ConfigFactory
import com.cloudera.flume.core.Event
import com.cloudera.flume.handlers.debug.{MemorySinkSource, StubbornAppendSink}
import com.cloudera.flume.handlers.thrift.ThriftEventSink
import com.cloudera.flume.reporter.aggregator.CounterSink
import org.specs2.mutable.{After, Specification}
import org.specs2.specification.Scope

object FlumeSinkEventHandlerListenerSpec extends Specification {
  sequential

  "Event Handler with a flume sink" should {
    "send events to a sink" in new sinkSource {
      log.info("hello")

      val nextEvent = getNextEvent(sink)
      nextEvent() must eventually (beSome)

      val event = nextEvent().get
      new String(event.get("sender")) must_== sourceName
      new String(event.get("senderClass")) must contain("FlumeSinkEventHandlerListenerSpec$")
      event.getPriority must_== Event.Priority.INFO
      new String(event.get("threadName")) must not (beEmpty)
      event.getTimestamp must beGreaterThan(0L)
      new String(event.getBody) must_== "hello"
    }

    "add stacktraces for error events" in new sinkSource {
      log.error(new RuntimeException("ouch")," hello")

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

      log.bus.publish(Logging.Info("me", classOf[Cat], Cat("lou")))

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

      log.bus.publish(Logging.Info("me", classOf[Cat], Cat("lou")))

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

      log.bus.publish(Logging.Info("me", classOf[Cat], Cat("lou")))

      val nextEvent = getNextEvent(sink)
      nextEvent() must eventually (beSome)

      val event = nextEvent().get
      event.getPriority must_== Event.Priority.INFO
      new String(event.get("world")) must_== "Earth"
      new String(event.get("type")) must_== "Cat"
      new String(event.get("sound")) must_== "meow"
    }

    "allow disk failover" in new diskFailoverDecoSink {
      log.info("ouch") must not (throwA[Exception])
    }
  }

  trait sinkSource extends Scope with After {
    self =>

    lazy val sink = FlumeSinkEventHandlerListener.memorySink
    lazy val sourceName = "sourceName"

    lazy val system = ActorSystem("sys", ConfigFactory.parseString("""
      akka {
        event-handlers = ["com.banno.akka.event.flume.FlumeSinkEventHandler"]
        logLevel = DEBUG
        flume-event-handler.sink = memory
      }
    """))

    val logSource = new LogSource[Any] {
      def genString(t: Any) = sourceName
    }

    lazy val log = Logging(system, self)(logSource).asInstanceOf[BusLogging]

    def getNextEvent(sink: MemorySinkSource): () => Option[Event] = {
      var nextEvent: Option[Event] = None
      () => nextEvent orElse Option(sink.next) map { ev =>
        nextEvent = Some(ev)
        ev
      }
    }

    def after = {
      FlumeEventDecorators.clear
    }
  }

  trait diskFailoverDecoSink extends sinkSource {
    override lazy val system = ActorSystem("sys", ConfigFactory.parseString("""
      akka {
        event-handlers = ["com.banno.akka.event.flume.FlumeSinkEventHandler"]
        logLevel = DEBUG
        flume-event-handler.sink = "diskFailover stubbornAppend rpcSink(\"localhost\", 35869)"
      }
    """))
  }
}

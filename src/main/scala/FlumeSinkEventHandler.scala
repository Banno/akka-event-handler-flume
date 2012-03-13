package com.banno.akka.event.flume
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import com.cloudera.flume.core.EventSink
import com.cloudera.flume.conf.{Context, FlumeBuilder, LogicalNodeContext}
import com.cloudera.flume.handlers.debug.MemorySinkSource

class FlumeSinkEventHandler extends Actor {
  import FlumeSinkEventHandlerListener._
  var sink: EventSink = _

  def receive = {
    case Logging.InitializeLogger(_) =>
      sink = configuredSink(context.system)
      sender ! Logging.LoggerInitialized
    case e: Logging.LogEvent =>
      val event = EventHandlerFlumeEvent(e)
      if (event.message != null)
        FlumeEventDecorators.decorateEvent(event.message, event)
      tryToWriteToSink(event)
    case genericEvent => // ignore
  }

  private def tryToWriteToSink(event: EventHandlerFlumeEvent): Unit = {
    try sink.append(event)
    catch {
      case t: Throwable =>
        println("Unable to append to sink: " + sink)
        println("Trying to reopen sink...")
        try reOpenSink()
        catch {
          case t: Throwable =>
            println("Unable to reopen sink!")
            println("Stopping sink.")
            context.stop(self)
            return
        }
        println("Sink reopened.")
        sink.append(event)
    }
  }

  override def postStop() {
    closeSink()
  }

  def openSink(): Unit = {
    if (sink != null) {
      sink.open
    }
  }

  def closeSink(): Unit = {
    if (sink != null) {
      sink.close
    }
  }

  private def reOpenSink() = {
    closeSink()
    openSink()
  }
}

object FlumeSinkEventHandlerListener {
  private[flume] def configuredSink(system: ActorSystem) = sinkFor(getConfiguredSinkValue(system).getOrElse("console"))

  private[flume] def sinkFor(sinkFlumeSpec: String) =
    if (sinkFlumeSpec == "memory") {
      memorySink
    } else {
      FlumeBuilder.buildSink(new LogicalNodeContext(Context.EMPTY, "akka-flume-event-handler", "localhost"), sinkFlumeSpec)
    }

  private[flume] def getConfiguredSinkValue(system: ActorSystem): Option[String] = {
    val config = system.settings.config
    if (config.hasPath("akka.flume-event-handler.sink")){
      Some(config.getString("akka.flume-event-handler.sink").replaceAll("\\\\\"", "\""))
    } else {
      None
    }
  }

  implicit def str2Bytes(str: String): Array[Byte] = if (str != null) str.getBytes else new Array(0)

  private[flume] lazy val memorySink = new MemorySinkSource
}


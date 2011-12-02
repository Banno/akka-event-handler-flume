package com.banno.akka.event.flume
import akka.actor.{Actor, ActorRef}
import akka.config.Config._
import akka.dispatch.Dispatchers
import akka.event.EventHandler
import com.cloudera.flume.core.EventSink
import com.cloudera.flume.conf.{Context, FlumeBuilder}

class FlumeSinkEventHandlerListener(sink: EventSink) extends Actor {
  def this() = this(FlumeSinkEventHandlerListener.configuredSink)
  def this(sinkFlumeSpec: String) = this(FlumeSinkEventHandlerListener.sinkFor(sinkFlumeSpec))

  self.dispatcher = FlumeSinkEventHandlerListener.dispatcher

  def receive = {
    case e: EventHandler.Event =>
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
            self.stop
            return
        }
        println("Sink reopened.")
        sink.append(event)
    }
  }

  private def reOpenSink() = {
    sink.close
    sink.open
  }

  override def preStart() {
    sink.open
  }

  override def postStop() {
    sink.close
  }
}

object FlumeSinkEventHandlerListener {

  def listenerFor(sinkFlumeSpec: String): ActorRef = listenerFor(sinkFor(sinkFlumeSpec))
  def listenerFor(sink: EventSink): ActorRef = Actor.actorOf(new FlumeSinkEventHandlerListener(sink)).start

  def addListenerSink(sinkFlumeSpec: String): Unit = addListenerSink(sinkFor(sinkFlumeSpec))
  def addListenerSink(sink: EventSink): Unit = EventHandler.addListener(listenerFor(sink))

  def addListenerPool(sinkFlumeSpec: String): Unit = EventHandler.addListener {
    Actor.actorOf(new FlumeSinkEventHandlerListenerPool(() => sinkFor(sinkFlumeSpec))).start
  }

  private[flume] def sinkFor(sinkFlumeSpec: String) = FlumeBuilder.buildSink(new Context, sinkFlumeSpec)

  private[flume] def configuredSink = sinkFor(config.getString("akka.flume-event-handler.sink", "console").replaceAll("\\\\\"", "\""))

  lazy val dispatcher = Dispatchers.newExecutorBasedEventDrivenDispatcher("akka-event-flume-listener").build

  implicit def str2Bytes(str: String): Array[Byte] = if (str != null) str.getBytes else new Array(0)
}


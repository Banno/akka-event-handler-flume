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
      FlumeEventDecorators.decorateEvent(event.message, event)
      sink.append(event)
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

  private[flume] def sinkFor(sinkFlumeSpec: String) = FlumeBuilder.buildSink(new Context, sinkFlumeSpec)

  private[flume] def configuredSink = sinkFor(config.getString("akka.flume-event-handler.sink", "console").replaceAll("\\\\\"", "\""))

  lazy val dispatcher = Dispatchers.newExecutorBasedEventDrivenDispatcher("akka-event-flume-listener").build

  implicit def str2Bytes(str: String): Array[Byte] = str.getBytes
}


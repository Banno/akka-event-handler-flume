package com.banno.akka.event.flume
import akka.actor.{Actor, ActorRef}
import akka.config.Config._
import akka.event.EventHandler
import java.io.{StringWriter, PrintWriter}
import com.cloudera.flume.core.{Event, EventImpl, EventSink}
import com.cloudera.flume.conf.{Context, FlumeBuilder}

case class EventHandlerFlumeEvent(event: EventHandler.Event) extends EventImpl {
  import EventHandler._
  import FlumeSinkEventHandlerListener._
  
  event match {
    case Error(cause, instance, message) =>
      setPriority(Event.Priority.ERROR)
      set("sender", instance.getClass.getName)
      set("exceptionType", cause.getClass.getName)
      set("exceptionMessage", cause.getMessage)
      set("exceptionBacktrace", printStackTrace(cause))
    case Warning(instance, message) =>
      setPriority(Event.Priority.WARN)
      set("sender", instance.getClass.getName)
    case Info(instance, message) =>
      setPriority(Event.Priority.INFO)
      set("sender", instance.getClass.getName)
    case Debug(instance, message) =>
      setPriority(Event.Priority.DEBUG)
      set("sender", instance.getClass.getName)
  }

  private def printStackTrace(cause: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    cause.printStackTrace(pw)
    sw.toString
  }
}

class FlumeSinkEventHandlerListener(sink: EventSink) extends Actor {
  def this() = this(FlumeSinkEventHandlerListener.configuredSink)
  def this(sinkFlumeSpec: String) = this(FlumeSinkEventHandlerListener.sinkFor(sinkFlumeSpec))
  
  def receive = {
    case e: EventHandler.Event => sink.append(EventHandlerFlumeEvent(e))
  }

  override def preStart() {
    sink.open
  }
  
}

object FlumeSinkEventHandlerListener {

  def listenerFor(sinkFlumeSpec: String): ActorRef = listenerFor(sinkFor(sinkFlumeSpec))
  def listenerFor(sink: EventSink): ActorRef = Actor.actorOf(new FlumeSinkEventHandlerListener(sink)).start
    
  def addListenerSink(sinkFlumeSpec: String): Unit = addListenerSink(sinkFor(sinkFlumeSpec))
  def addListenerSink(sink: EventSink): Unit = EventHandler.addListener(listenerFor(sink))

  private[event] def sinkFor(sinkFlumeSpec: String) = FlumeBuilder.buildSink(new Context, sinkFlumeSpec)
  
  private[event] def configuredSink = sinkFor(config.getString("akka.flume-event-handler-sink", "console"))
  
  implicit def str2Bytes(str: String): Array[Byte] = str.getBytes
}
  

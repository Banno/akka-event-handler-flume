package com.banno.akka.event.flume
import akka.event.EventHandler
import java.io.{StringWriter, PrintWriter}
import com.cloudera.flume.core.{Event, EventImpl}
import scala.annotation.switch

case class EventHandlerFlumeEvent(event: EventHandler.Event) extends EventImpl {
  import EventHandler._
  import FlumeSinkEventHandlerListener._
  
  set("threadName", event.thread.getName)
  
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

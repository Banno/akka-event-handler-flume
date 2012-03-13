package com.banno.akka.event.flume
import akka.event.Logging
import java.io.{StringWriter, PrintWriter}
import com.cloudera.flume.core.{Event, EventImpl}

case class EventHandlerFlumeEvent(event: Logging.LogEvent) extends FlumeEvent {
  import Logging._
  import FlumeSinkEventHandlerListener._

  set("threadName", event.thread.getName)

  val message = event match {
    case Error(cause, logSource, logClass, message) =>
      setPriority(Event.Priority.ERROR)
      set("sender", logSource)
      set("senderClass", logClass.getName)
      set("exceptionType", cause.getClass.getName)
      set("exceptionMessage", cause.getMessage)
      set("exceptionBacktrace", printStackTrace(cause))
      message
    case Warning(logSource, logClass, message) =>
      setPriority(Event.Priority.WARN)
      set("sender", logSource)
      set("senderClass", logClass.getName)
      message
    case Info(logSource, logClass, message) =>
      setPriority(Event.Priority.INFO)
      set("sender", logSource)
      set("senderClass", logClass.getName)
      message
    case Debug(logSource, logClass, message) =>
      setPriority(Event.Priority.DEBUG)
      set("sender", logSource)
      set("senderClass", logClass.getName)
      message
  }

  override val getBody: Array[Byte] =
    if (message != null) {
      message.toString.getBytes
    } else {
      Array.empty
    }

  override val getTimestamp = System.currentTimeMillis

  private def printStackTrace(cause: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    cause.printStackTrace(pw)
    sw.toString
  }
}

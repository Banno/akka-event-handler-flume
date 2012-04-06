package com.banno.akka.event.flume
import akka.actor.{Actor, ActorRef}
import akka.config.Config._
import akka.dispatch.Dispatchers
import akka.event.EventHandler
import com.cloudera.flume.core.EventSink
import com.cloudera.flume.conf.{Context, FlumeBuilder, LogicalNodeContext}

class FlumeSinkEventHandlerListener(sink: EventSink, levelThreshold: Int = EventHandler.DebugLevel) extends Actor {
  def this() = this(FlumeSinkEventHandlerListener.configuredSink)
  def this(sinkFlumeSpec: String, nodeName: String = "localhost") = this(FlumeSinkEventHandlerListener.sinkFor(sinkFlumeSpec, nodeName))

  self.dispatcher = FlumeSinkEventHandlerListener.dispatcher

  def receive = {
    case e: EventHandler.Event if e.level <= levelThreshold =>
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

  def listenerFor(sinkFlumeSpec: String, nodeName: String = "localhost"): ActorRef = listenerFor(sinkFor(sinkFlumeSpec, nodeName))
  def listenerFor(sink: EventSink): ActorRef = Actor.actorOf(new FlumeSinkEventHandlerListener(sink)).start
  def listenerFor(sink: EventSink, levelThreshold: Int): ActorRef = Actor.actorOf(new FlumeSinkEventHandlerListener(sink, levelThreshold)).start

  def addListenerSink(sinkFlumeSpec: String, nodeName: String): Unit = addListenerSink(sinkFor(sinkFlumeSpec, nodeName))
  def addListenerSink(sinkFlumeSpec: String, levelThreshold: Int, nodeName: String): Unit = addListenerSink(sinkFor(sinkFlumeSpec, nodeName), levelThreshold)
  def addListenerSink(sink: EventSink): Unit = EventHandler.addListener(listenerFor(sink))
  def addListenerSink(sink: EventSink, levelThreshold: Int): Unit = EventHandler.addListener(listenerFor(sink, levelThreshold))

  def addListenerPool(sinkFlumeSpec: String, nodeName: String = "localhost"): Unit = EventHandler.addListener {
    Actor.actorOf(new FlumeSinkEventHandlerListenerPool(() => sinkFor(sinkFlumeSpec, nodeName))).start
  }

  private[flume] def sinkFor(sinkFlumeSpec: String, nodeName: String) = FlumeBuilder.buildSink(new LogicalNodeContext(Context.EMPTY, "akka-flume-event-handler", nodeName), sinkFlumeSpec)

  private[flume] def configuredSink = sinkFor(config.getString("akka.flume-event-handler.sink", "console").replaceAll("\\\\\"", "\""),
                                              config.getString("akka.flume-event-handler.node-name", "localhost"))

  lazy val dispatcher = Dispatchers.newExecutorBasedEventDrivenDispatcher("akka-event-flume-listener").build

  implicit def str2Bytes(str: String): Array[Byte] = if (str != null) str.getBytes else new Array(0)
}


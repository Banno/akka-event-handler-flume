package com.banno.akka.event.flume
import akka.actor.Actor
import akka.config.Config._
import akka.routing.{DefaultActorPool, SmallestMailboxSelector, BasicNoBackoffFilter, BoundedCapacityStrategy, MailboxPressureCapacitor}
import com.cloudera.flume.core.EventSink

class FlumeSinkEventHandlerListenerPool(sink: EventSink) extends Actor
with DefaultActorPool
with BoundedCapacityStrategy
with SmallestMailboxSelector
with MailboxPressureCapacitor
with BasicNoBackoffFilter {
  def this() = this(FlumeSinkEventHandlerListener.configuredSink)

  def receive = _route
  def lowerBound = 1
  def upperBound = config.getInt("akka.flume-event-handler.upper-bound", 20)
  def rampupRate = 0.1
  def pressureThreshold = 50
  def partialFill = true
  def selectionCount = 1
  def instance = Actor.actorOf(new FlumeSinkEventHandlerListener(sink))
}

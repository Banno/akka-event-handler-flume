package com.banno.akka.event.flume
import com.cloudera.flume.core.{Event, EventImpl}

class FlumeEvent extends EventImpl {
  def update(key: String, value: Array[Byte]): Unit = set(key, value)
  def update(key: String, value: String): Unit = update(key, value.getBytes)
}

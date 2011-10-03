package com.banno.akka.event.flume
import scala.collection.mutable

object FlumeEventDecorators {
  private val decorators = mutable.HashMap.empty[Class[_], Function2[_, FlumeEvent, Unit]]

  def decorate[T](f: (T, FlumeEvent) => Unit)(implicit m: Manifest[T]) = decorators(m.erasure) = f

  def decorateEvent(a: Any, ev: FlumeEvent) =
    decorators.get(a.getClass).foreach(_.asInstanceOf[Function2[Any, FlumeEvent, Unit]].apply(a, ev))
}

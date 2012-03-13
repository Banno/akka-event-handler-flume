package com.banno.akka.event.flume
import scala.collection.mutable

object FlumeEventDecorators {
  type Decorator[T] = Function2[T, FlumeEvent, Unit]

  def decorate[T](f: (T, FlumeEvent) => Unit)(implicit m: Manifest[T]) =
    decorators.put(m.erasure, List(f)) foreach { previous => decorators.put(m.erasure, f +: previous) }

  def decorateEvent(a: Any, ev: FlumeEvent) =
    decoratorsForClass(a.getClass).foreach(_.apply(a, ev))

  def clear {
    decorators.clear
    classToDecoratorCache.clear
  }

  private val decorators = new mutable.HashMap[Class[_], List[Decorator[_]]]

  private val classToDecoratorCache = new mutable.HashMap[Class[_], List[Decorator[Any]]]
  private def decoratorsForClass(clazz: Class[_]): List[Decorator[Any]] =
    classToDecoratorCache.getOrElseUpdate(clazz,
                                          decorators.filterKeys(_.isAssignableFrom(clazz)).values.flatten.toList.asInstanceOf[List[Decorator[Any]]])

}

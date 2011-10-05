# akka-event-handler-flume #

`akka-event-handler-flume` is an [akka][] `EventHandler` listener that can publish to [flume][] sinks. It is akin to the [akka-slf4j][] EventHandler. See akka's [event-handler][] documentation for more information. [T8 Webware][] has been using this to great success.

## Usage ##

### Registration of the listener ###

There are a couple of different ways to register the `FlumeSinkEventHandlerListener`

* Through the `akka.conf`

```ruby
akka {
  event-handlers = ["com.banno.akka.event.flume.FlumeSinkEventHandlerListener"]
  flume-event-handler.sink = "console"
}
```

* Explicitly through scala code

```scala
import com.banno.akka.event.flume._

val listener = FlumeSinkEventHandlerListener.listenerFor("{ bulk(100) => rpcSource(\"collector\", 12345) }")
EventHandler.addListener(listener)
```

or more succintly

```scala
import com.banno.akka.event.flume._

FlumeSinkEventHandlerListener.addListener("{ bulk(100) => rpcSource(\"collector\", 12345) }")
```

### Configuring the Sink ###

The `FlumeSinkEventHandlerListener` can take in either 

* an explicit [flume][] EventSink
* a string as long as it follow the same grammar as the [flume sink spec][]

These things can be passed in through the constructor argument or the `akka.flume-event-handler.sink` config property in the `akka.conf`. 
*Please* take the time to familiarize yourself with the different types of decorators and event sinks. There are many durability gurantees that they can give you.

### Decorating the flume event with semi-structured data ###

One of the greater things about logging to Flume is that you can have semi-structured data instead of just a body text. Some of things you can do with this are: nodes down the line can organize and redirect to a different sink based of an attribute, MapReduce jobs can also take advantage of the structured data instead of tokenizing a string. 

The way to do this is like:
```scala
trait Animal
case class Cat(name: String) extends Animal

FlumeEventDecorators.decorate { (c: Cat, ev: FlumeEvent) =>
  ev("catName") = c.name
}
 
EventHandler.info(this, Cat("lou"))
```

The [flume][] event sent will now have a `catName` attribute set to the cat's name. This can also work for superclasses and traits. Multiple decorators can be set. 

There are some standard attributes that the `FlumeSinkEventHandlerListener` will set as well.

* `sender`: the sender of the `EventHandler` log message
* `threadName`: the thread name that sent the log message
* `exceptionType`: if an error, the type of the throwable
* `exceptionMessage`: if an error, the message of the throwable
* `exceptionBacktrace`: if an error, the backtrace of the throwable

The [flume][] event priority will be the priority of the `EventHandler` event.

### Greater Throughput ###

If you find yourself logging so much that your `FlumeSinkEventHandlerListener`'s queue backed up so much that it causes an `OutOfMemoryException` (we have), then there is also a `FlumeSinkEventHandlerListenerPool` which will uses akka's `ActorPool` implementation to ramp up and grow when mailboxes come under pressure. It will grow up to the config property `akka.flume-event-handler.upper-bound` or will default to 20.

## License ##

This is licensed under the Apache License Version 2.0

[akka]: http://akka.io
[flume]: https://cwiki.apache.org/FLUME/
[akka-sl4jf]: http://akka.io/docs/akka/1.2/general/slf4j.html
[event-handler]: http://akka.io/docs/akka/1.2/general/event-handler.html
[flume sink spec]: http://archive.cloudera.com/cdh/3/flume/UserGuide/index.html#_introducing_sinks
[T8 Webware]: http://grabgrip.com

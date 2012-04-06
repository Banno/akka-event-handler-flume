organization := "com.banno"

name := "akka-event-handler-flume"

version := "1.0-SNAPSHOT"

scalaVersion := "2.9.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Cloudera Repository" at "https://repository.cloudera.com/content/groups/public/",
                  "Cloudera thirdparty" at "https://repository.cloudera.com/content/repositories/third-party",
                  "Banno Third Party" at "http://nexus.banno.com/nexus/content/repositories/thirdparty/",
                  "dtrott" at "http://maven.davidtrott.com/repository",
                  "jboss" at "http://repository.jboss.org/nexus/content/groups/public/")

libraryDependencies ++= Seq("com.cloudera" % "flume-core" % "0.9.4-cdh3u1",
                            "se.scalablesolutions.akka" % "akka-actor" % "1.1.3")

libraryDependencies ++= Seq("org.specs2" %% "specs2" % "1.6.1" % "test",
                            "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test")

publishTo <<= (version) { version: String =>
  val nexus = "http://nexus.banno.com/nexus/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/") 
  else                                   Some("releases"  at nexus + "releases/")
}                            

credentials += Credentials(Path.userHome / ".ivy2" / ".banno_credentials")

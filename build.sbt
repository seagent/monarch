name := "QueryMonitor"

version := "0.1"

scalaVersion := "2.12.6"

val jenaVersion = "2.11.0"

val akkaVersion = "2.5.31"

val gJsonVersion = "2.8.2"

val jsonVersion = "2.6.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.apache.jena" % "jena-arq" % jenaVersion,
  "org.apache.jena" % "jena-core" % jenaVersion,
  "com.typesafe.play" %% "play-json" % jsonVersion,
  "net.debasishg" %% "redisclient" % "3.7",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "io.kamon" % "sigar-loader" % "1.6.6-rev002"
).map(_ exclude("org.slf4j", "*"))
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

//libraryDependencies ++= Seq(
//  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.103",
//  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % "0.103" % Test
//)

//resolvers += Resolver.sonatypeRepo("snapshots")

enablePlugins(JavaAppPackaging)
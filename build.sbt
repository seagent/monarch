
name := "QueryMonitor"

version := "0.1"

scalaVersion := "2.12.6"

val jenaVersion="3.7.0"

val akkaVersion = "2.5.13"

val gJsonVersion="2.8.2"

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "io.kamon" % "sigar-loader" % "1.6.6-rev002",
      "org.apache.jena" % "jena-arq" % jenaVersion,
      "org.apache.jena" % "jena-core" % jenaVersion,
      "com.google.code.gson" % "gson" % gJsonVersion,
      "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.6.7",
      "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % "0.6.7",
      "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.7",
      "com.trueaccord.scalapb" %% "scalapbc" % "0.6.7",
      "com.typesafe.play" %% "play-json" % "2.6.7"
    )

resolvers += Resolver.bintrayRepo("julien-lafont", "maven")

libraryDependencies ++= Seq(
  "io.protoless" %% "protoless-core" % "0.0.7",
  "io.protoless" %% "protoless-generic" % "0.0.7"
)

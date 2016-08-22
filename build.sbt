name := "akka-sample-main-scala"

version := "0.0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.9",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.9",
  "com.typesafe.akka" %% "akka-cluster-metrics" % "2.4.9",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.9"
)


import AssemblyKeys._

assemblySettings

name := "irc"

version := "0.5.2-SNAPSHOT"

organization := "org.conbere"

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("http://github.com/aconbere/scala-irc"))

scalaVersion := "2.11.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

fork in run := false

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

testOptions in Test += Tests.Argument("-oDF")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.0" % "test"
, "junit" % "junit" % "4.10" % "test"
, "com.typesafe.akka" %% "akka-actor" % "2.3.3"
, "com.typesafe.akka" %% "akka-contrib" % "2.3.3"
, "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
, "com.typesafe" % "config" % "1.0.0"
, "org.parboiled" %% "parboiled" % "2.0.0-RC2"
, "org.slf4j" % "slf4j-simple" % "1.7.7" % "test"
)

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
  } else {
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  }
}

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>git@github.com:aconbere/scala-irc.git</url>
    <connection>scm:git:git@github.com:aconbere/scala-irc.git</connection>
  </scm>
  <developers>
    <developer>
      <id>aconbere</id>
      <name>Anders Conbere</name>
      <url>http://anders.conbere.org</url>
    </developer>
  </developers>)
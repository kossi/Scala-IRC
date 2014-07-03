import AssemblyKeys._

assemblySettings

name := "irc-perf"

version := "0.0.3-SNAPSHOT"

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))


scalaVersion := "2.11.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

libraryDependencies ++= Seq(
 "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
)

publishArtifact := false

publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))

cappiSettings

lazy val scalaIRC = RootProject( file("../.") )

lazy val root = Project(id = "irc-perf", base = file("."))
  .dependsOn(scalaIRC)



import AssemblyKeys._

assemblySettings

name := "irc-perf"

version := "0.0.2-SNAPSHOT"

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))


scalaVersion := "2.11.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

libraryDependencies ++= Seq(
 "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
)

val noPub = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))))

lazy val scalaIRC = RootProject( file("../.") )

lazy val root = Project(id = "irc-perf", base = file("."))
  .dependsOn(scalaIRC)
  .settings(noPub: _*)



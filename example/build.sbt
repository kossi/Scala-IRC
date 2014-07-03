name := "irc-example"

version := "0.0.1-SNAPSHOT"

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))


scalaVersion := "2.11.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

publishArtifact := false

publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))

lazy val scalaIRC = RootProject( file("../.") )

lazy val root = Project(id = "irc-example", base = file("."))
  .dependsOn(scalaIRC)



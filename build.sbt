import Dependencies._

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.35.0"

libraryDependencies += "org.apache.commons" % "commons-email" % "1.5"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.2"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "fs.tools.luxmed.monitor",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "luxmed-monitor",
    libraryDependencies += scalaTest % Test
  )

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

libraryDependencies ++= List(
  "com.softwaremill.sttp.client3" %% "core" % "3.9.0",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.9.0",
  "org.jsoup" % "jsoup" % "1.14.3",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  "org.typelevel" %% "cats-effect" % "3.5.2",
  "org.typelevel" %% "cats-core" % "2.9.0",
  "io.circe" %% "circe-parser" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "org.scalameta" %% "munit" % "0.7.29" % Test,
  "com.github.tototoshi" %% "scala-csv" % "1.3.10",
  "org.scalafx" %% "scalafx" % "20.0.0-R31",
  "org.scalafx" %% "scalafx-extras" % "0.8.0"
)

lazy val root = (project in file("."))
  .settings(
    name := "GradeTranslator"
  )

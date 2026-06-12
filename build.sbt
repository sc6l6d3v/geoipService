import Dependencies._

lazy val root = (project in file("."))
  .settings(
    organization := "com.iscs",
    name := "geoipService",
    version := "0.1-SNAPSHOT",
    scalaVersion := "3.3.8",
    scalacOptions ++= Seq("-release:17"),
    libraryDependencies ++= Seq(
      http4s.server,
      http4s.dsl,
      sttp.client3,
      zio.json,
      redis4cats.core,
      redis4cats.stream,
      redis4cats.log4cats,
      mongo4cats.core,
      mongo4cats.circe,
      specs2.test,
      weaverTest.cats,
      logback.classic,
      logback.logging,
      cats.log4cats
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    Revolver.enableDebugging(5050, suspend = true)
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-source:3.3-migration"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

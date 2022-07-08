import Dependencies._

lazy val root = (project in file("."))
  .settings(
    organization := "com.iscs",
    name := "geoipService",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      http4s.blaze_server,
      http4s.dsl,
      sttp.client3,
      zio.json,
      redis4cats.core,
      redis4cats.stream,
      redis4cats.log4cats,
      mongo4cats.core,
      mongo4cats.circe,
      mongodb.driver,
      specs2.test,
      logback.classic,
      logback.logging,
      cats.retry,
      cats.log4cats,
      fs2.core,
      fs2.io,
      fs2.streams
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    Revolver.enableDebugging(5050, true)
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature"
  //"-Xfatal-warnings",
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

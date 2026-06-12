import sbt._

object Dependencies {
  object Versions {
    val Http4sVersion = "0.23.32"
    val Specs2Version = "5.9.0"
    val LogbackVersion = "1.5.34"
    val log4catsVersion = "2.8.0"
    val loggingVersion = "3.9.5"
    val redis4catsVersion = "1.7.2"
    val mongo4catsVersion = "0.7.13"
    val zioJsonVersion = "0.7.45"
    val sttpVersion = "3.9.8"
    val WeaverTestVersion = "0.12.0"
  }

  object http4s {
    val server       = "org.http4s"  %% "http4s-ember-server" % Versions.Http4sVersion
    val dsl          = "org.http4s"  %% "http4s-dsl"          % Versions.Http4sVersion
  }

  object sttp {
    val client3 = "com.softwaremill.sttp.client3" %% "fs2" % Versions.sttpVersion
  }

  object zio {
    val json = "dev.zio" %% "zio-json" % Versions.zioJsonVersion
  }

  object redis4cats {
    val core     = "dev.profunktor" %% "redis4cats-effects"  % Versions.redis4catsVersion
    val stream   = "dev.profunktor" %% "redis4cats-streams"  % Versions.redis4catsVersion
    val log4cats = "dev.profunktor" %% "redis4cats-log4cats" % Versions.redis4catsVersion
  }

  object mongo4cats {
    val core  = "io.github.kirill5k" %% "mongo4cats-core"  % Versions.mongo4catsVersion
    val circe = "io.github.kirill5k" %% "mongo4cats-circe" % Versions.mongo4catsVersion
  }

  object weaverTest {
    val cats  = "org.typelevel" %% "weaver-cats" % Versions.WeaverTestVersion % "test"
  }

  object specs2 {
    val test = "org.specs2" %% "specs2-core" % Versions.Specs2Version % "test"
  }

  object logback {
    val classic = "ch.qos.logback"             %  "logback-classic" % Versions.LogbackVersion
    val logging = "com.typesafe.scala-logging" %% "scala-logging"   % Versions.loggingVersion
  }

  object cats {
    val log4cats = "org.typelevel" %% "log4cats-slf4j" % Versions.log4catsVersion
  }
}

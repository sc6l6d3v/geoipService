import sbt._

object Dependencies {
  object Versions {
    val Http4sVersion = "1.0.0-M21"
    val Http4sClientVersion = "0.23.11"
    val CirceVersion = "0.14.1"
    val Specs2Version = "4.9.3"
    val LogbackVersion = "1.2.3"
    val catsRetryVersion = "1.1.0"
    val log4catsVersion = "2.3.1"
    val fs2Version = "3.2.8"
    val loggingVersion = "3.9.2"
    val redis4catsVersion = "1.2.0"
    val upickleVersion = "0.9.5"
    val fs2MongoVersion = "0.5.0"
    val mongoScalaVersion = "4.2.3"
    val mongo4catsVersion = "0.4.7"
    val zioJsonVersion = "0.1.5"
    val zioJsonHttp4sVersion = "0.1.5"
    val zioInteropCats3Version = "3.3.0-RC7"
    val sttpVersion = "3.5.2"
  }

  object http4s {
    val blaze_server = "org.http4s"       %% "http4s-blaze-server" % Versions.Http4sVersion
    val blaze_client = "org.http4s"       %% "http4s-blaze-client" % Versions.Http4sVersion
    val circe = "org.http4s"       %% "http4s-circe"        % Versions.Http4sVersion
    val dsl =    "org.http4s"  %% "http4s-dsl"          % Versions.Http4sVersion
//    val async_http_client = "org.http4s"       %% "http4s-async-http-client" % Versions.Http4sClientVersion
  }

  object sttp {
    val client3 = "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2" % Versions.sttpVersion
    //    val akka_backend = "com.softwaremill.sttp" %% "akka-http-backend" % Versions.sttpVersion
  }

  object zio {
    val json = "dev.zio" %% "zio-json" % Versions.zioJsonVersion
    val interop_cats = "dev.zio" %% "zio-interop-cats" % Versions.zioInteropCats3Version
    val interop_http4s = "dev.zio" %% "zio-json-interop-http4s" % Versions.zioJsonHttp4sVersion
  }

  object circe {
    val generic = "io.circe"   %% "circe-generic"       % Versions.CirceVersion
    val parser = "io.circe"    %% "circe-parser"        % Versions.CirceVersion
    val optics = "io.circe"    %% "circe-optics"        % Versions.CirceVersion
  }

  object redis4cats {
    val core = "dev.profunktor" %% "redis4cats-effects" % Versions.redis4catsVersion
    val stream = "dev.profunktor" %% "redis4cats-streams" % Versions.redis4catsVersion
    val log4cats = "dev.profunktor" %% "redis4cats-log4cats" % Versions.redis4catsVersion
  }

   object mongodb {
    val driver = "org.mongodb.scala" %% "mongo-scala-driver" % Versions.mongoScalaVersion
  }

  object mongo4cats {
    val core = "io.github.kirill5k" %% "mongo4cats-core" % Versions.mongo4catsVersion
    val circe = "io.github.kirill5k" %% "mongo4cats-circe" % Versions.mongo4catsVersion
  }

  object specs2 {
    val test = "org.specs2"       %% "specs2-core"         % Versions.Specs2Version % "test"
  }

  object logback {
    val classic = "ch.qos.logback"   %  "logback-classic"     % Versions.LogbackVersion
    val logging = "com.typesafe.scala-logging" %% "scala-logging" % Versions.loggingVersion
  }

  object cats {
    val retry = "com.github.cb372" %% "cats-retry"          % Versions.catsRetryVersion
    val log4cats = "org.typelevel" %% s"log4cats-slf4j" % Versions.log4catsVersion
  }

  object fs2 {
    val core = "co.fs2"        %% "fs2-core"            % Versions.fs2Version
    val io =    "co.fs2"       %% "fs2-io"              % Versions.fs2Version
    val streams = "co.fs2"     %% "fs2-reactive-streams" % Versions.fs2Version
  }
}

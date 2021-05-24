package com.iscs.geoip

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.iscs.geoip.config.RedisConfig
import com.iscs.geoip.util.{DbClient, Mongo}
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log.Stdout._
import dev.profunktor.redis4cats.{Redis, RedisCommands}

object Main extends IOApp {
  private val L = Logger[this.type]

  def run(args: List[String]): IO[ExitCode] = for {
    start <- IO.delay(System.currentTimeMillis)
    resources = for {
      redis <- for {
        uri <- RedisConfig().uri
        cli <- RedisClient[IO](uri)
        cmd <- Redis[IO].fromClient(cli, RedisCodec.Utf8)
      } yield cmd
      mongoClient <- Resource.fromAutoCloseable(DbClient[IO](Mongo.fromUrl(), List("elections-2016", "covid-state")))
    } yield (redis, IO.delay(mongoClient))

    ec <- resources.use { case (cmd, dbClient) =>
      implicit val redisCmd: RedisCommands[IO, String, String] = cmd
      for {
        start <- IO.delay(System.currentTimeMillis)
        serverStream = for {
          str <- GeoIPServer.stream[IO](dbClient)
        } yield str

        s <- serverStream
          .compile.drain.as(ExitCode.Success)
          .handleErrorWith(ex => IO {
            L.error("\"exception during stream startup\" exception={} ex={}", ex.toString, ex)
            ExitCode.Error
          })
      } yield s
    }
  } yield ec
}
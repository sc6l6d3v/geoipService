package com.iscs.geoip

import cats.effect.{ExitCode, IO, IOApp, Resource, Sync}
import com.iscs.geoip.config.RedisConfig
import com.iscs.geoip.util.{DbClient, Mongo}
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log.Stdout._
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend

object Main extends IOApp {
  private val L = Logger[this.type]
  private val dbName = sys.env.getOrElse("DBNAME", "db")
  private val collName = sys.env.getOrElse("COLLNAME", "ipdb")

  def run(args: List[String]): IO[ExitCode] = for {
    start <- IO.delay(System.currentTimeMillis)
    dbClient2 <- IO.delay(new DbClient[IO](dbName, List(collName), Mongo.fromUrl()))
    resources = for {
      redis    <- new RedisConfig[IO]().resource2
      db2      <- dbClient2.dbResource
      sttpRes   <- AsyncHttpClientFs2Backend.resource[IO]()
    } yield (redis, db2, sttpRes)

    ec <- resources.use { case (cmd, dbClient, sttpCli) =>
      implicit val redisCmd: RedisCommands[IO, String, String] = cmd
      for {
        _ <- IO.delay(L.info("starting service"))
        serverStream <- IO.delay(GeoIPServer.stream[IO](IO(dbClient2), sttpCli))
        s <- serverStream
          .compile.drain.as(ExitCode.Success)
          .handleErrorWith(ex => IO {
            L.error("\"exception during stream startup\" exception={} ex={}", ex.toString, ex)
            ExitCode.Error
          })
      } yield s
    }
    end <- IO.delay(System.currentTimeMillis)
  } yield ec
}
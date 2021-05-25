package com.iscs.geoip

import java.util.concurrent.Executors

import cats.effect.{Blocker, Concurrent, ConcurrentEffect, ContextShift, Sync, Timer}
import cats.implicits._
import com.iscs.geoip.domains.GeoIP
import com.iscs.geoip.routes.GeoIPRoutes
import com.iscs.geoip.util.{DbClient, ResourceProcessor}
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{Logger => hpLogger}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

object GeoIPServer {
  private val port = sys.env.getOrElse("PORT", "8080").toInt
  private val bindHost = sys.env.getOrElse("BINDHOST", "0.0.0.0")
  private val threadPool = sys.env.getOrElse("THREADPOOL", "16").toInt

  private val L = Logger[this.type]

  def getResource[F[_]: Sync: ContextShift: Timer: ConcurrentEffect](resName: String, blocker: Blocker): F[String] = for {
    resProc <- Concurrent[F].delay(new ResourceProcessor(resName))
    csvLines <- resProc.readLinesFromFile(blocker)
    _ <- Concurrent[F].delay(L.info("\"getting resource file\" file={} contents={} lines", resName, csvLines.length))
  } yield csvLines

  def stream[F[_]: ConcurrentEffect](mongoClient: F[DbClient[F]])
                                    (implicit cmd: RedisCommands[F, String, String], T: Timer[F], Con: ContextShift[F]):
  Stream[F, Nothing] = Stream.resource(Blocker[F]).flatMap { blocker =>

    val srvStream = for {
      client <- BlazeClientBuilder[F](global).stream
      mongo <- Stream.eval(mongoClient)
      covidAlg = GeoIP.impl[F](client, mongo)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = GeoIPRoutes.geoIPRoutes[F](covidAlg).orNotFound

      // With Middlewares in place
      finalHttpApp = hpLogger.httpApp(logHeaders = true, logBody = true)(httpApp)

      es <- Stream.eval(Concurrent[F].delay(Executors.newFixedThreadPool(threadPool)))
      ex <- Stream.eval(Concurrent[F].delay(ExecutionContext.fromExecutorService(es)))
      exitCode <- BlazeServerBuilder[F](ex)
        .bindHttp(port, bindHost)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
    srvStream.drain
  }
}
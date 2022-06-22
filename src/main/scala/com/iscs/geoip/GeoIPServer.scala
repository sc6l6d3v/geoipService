package com.iscs.geoip

import java.util.concurrent.Executors

import cats.effect.{Async, Sync}
import com.iscs.geoip.domains.GeoIP
import com.iscs.geoip.routes.GeoIPRoutes
import com.iscs.geoip.util.DbClient
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{Logger => hpLogger}
import sttp.capabilities
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

object GeoIPServer {
  private val port = sys.env.getOrElse("PORT", "8080").toInt
  private val bindHost = sys.env.getOrElse("BINDHOST", "0.0.0.0")
  private val serverPoolSize = sys.env.getOrElse("SERVERPOOL", "16").toInt
  private val clientPoolSize = sys.env.getOrElse("CLIENTPOOL", "16").toInt

  private val L = Logger[this.type]

  def getPool[F[_] : Sync](size: Int): Stream[F, ExecutionContextExecutorService] = for {
    es <- Stream.eval(Sync[F].delay(Executors.newFixedThreadPool(size)))
    ex <- Stream.eval(Sync[F].delay(ExecutionContext.fromExecutorService(es)))
  } yield ex

  def stream[F[_]: Async](mongoClient: F[DbClient[F]],
                          sttpClient: SttpBackend[F, Fs2Streams[F] with capabilities.WebSockets])
                                    (implicit cmd: RedisCommands[F, String, String]):
  Stream[F, Nothing] = {// Stream.resource(//Blocker[F]).flatMap { blocker =>

    val srvStream = for {
      clientPool <- getPool(clientPoolSize)
      client <- BlazeClientBuilder[F](clientPool).stream
      mongo <- Stream.eval(mongoClient)
      covidAlg = GeoIP.impl[F](client, mongo, sttpClient)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = GeoIPRoutes.geoIPRoutes[F](covidAlg).orNotFound

      // With Middlewares in place
      finalHttpApp = hpLogger.httpApp(logHeaders = true, logBody = true)(httpApp)

      serverPool <- getPool(serverPoolSize)
      exitCode <- BlazeServerBuilder[F](serverPool)
        .bindHttp(port, bindHost)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
    srvStream.drain

  }
}
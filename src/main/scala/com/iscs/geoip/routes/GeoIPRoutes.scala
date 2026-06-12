package com.iscs.geoip.routes

import cats.effect.Sync
import cats.syntax.all.*
import com.iscs.geoip.domains.GeoIP
import com.typesafe.scalalogging.Logger
import org.http4s.MediaType.application.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import zio.json.*

object GeoIPRoutes {
  private val L = Logger[this.type]

  def geoIPRoutes[F[_]: Sync](C: GeoIP[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      case _ @ GET -> Root / "ip" / ip =>
        Ok(for {
          grid <- C.getByIP(ip.toLowerCase)
          _ <- Sync[F].delay(L.info(s""""ip request" $ip"""))
          resp <- Sync[F].delay(grid.toJson)
        } yield resp).map(_.withContentType(`Content-Type`(`json`)))
    }
  }
}

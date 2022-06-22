package com.iscs.geoip.routes

import cats.effect.Sync
import cats.implicits._
import com.iscs.geoip.domains.GeoIP
import com.typesafe.scalalogging.Logger
import zio.json._
import org.http4s._
import org.http4s.dsl.Http4sDsl

object GeoIPRoutes {
  private val L = Logger[this.type]

  implicit val llSEncoder: JsonEncoder[List[List[String]]] = DeriveJsonEncoder.gen[List[List[String]]]

  def geoIPRoutes[F[_]: Sync](C: GeoIP[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case _ @ GET -> Root / "ip" / ip =>
        for {
          grid <- C.getByIP(ip.toLowerCase)
          _ <- Sync[F].delay(L.info(s""""ip request" $ip"""))
          resp <- Ok(grid.toJson)
        } yield resp
    }
  }
}

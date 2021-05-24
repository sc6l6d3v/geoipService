package com.iscs.covidbystates.routes

import cats.effect.Sync
import cats.implicits._
import com.iscs.covidbystates.domains.GeoIP
import com.typesafe.scalalogging.Logger
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object GeoIPRoutes {
  private val L = Logger[this.type]

  implicit val llSEncoder: Encoder[List[List[String]]] = deriveEncoder[List[List[String]]]
  implicit def llSEntityEncoder[F[_]: Sync]: EntityEncoder[F, List[List[String]]] =
    jsonEncoderOf

  def geoIPRoutes[F[_]: Sync](C: GeoIP[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "ip" / ip =>
        for {
          grid <- C.getByIP(ip.toLowerCase)
          resp <- Ok(grid)
        } yield resp
    }
  }
}

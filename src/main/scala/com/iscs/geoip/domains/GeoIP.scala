package com.iscs.geoip.domains

import cats.effect.{Concurrent, Sync}
import cats.implicits._
import com.iscs.geoip.covid.GeoIPApiUri
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{EntityDecoder, EntityEncoder, _}

trait GeoIP[F[_]] extends Cache[F] {
  def getByIP(state: String): F[GeoIP.IP]
}

object GeoIP {
  private val L = Logger[this.type]

  def apply[F[_]](implicit ev: GeoIP[F]): GeoIP[F] = ev

  final case class IP(ip: String, country_code2: String, country_code3: String, country_name: String,
                      state_prov: String, district: String, city: String, zipcode: String, latitude: String,
                      longitude: String, organization: String) {
    override def toString: String = s"$this"
  }

  object IP {
    implicit val ipDecoder: Decoder[IP] = deriveDecoder[IP]
    implicit def stateEntityDecoder[F[_]: Sync]: EntityDecoder[F, IP] = jsonOf
    implicit val stateEncoder: Encoder[IP] = deriveEncoder[IP]
    implicit def stateEntityEncoder[F[_]: Sync]: EntityEncoder[F, IP] = jsonEncoderOf

    def empty: IP = IP("", "", "", "", "", "", "", "", "", "", "")
  }

  final case class DataError(e: Throwable) extends RuntimeException

  def fromState(state: String): IP = parse(state).getOrElse(Json.Null).as[IP].getOrElse(IP.empty)

  def impl[F[_]: Concurrent: Sync](C: Client[F])
                                  (implicit cmd: RedisCommands[F, String, String]): GeoIP[F] = new GeoIP[F]{
    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F]{}
    import dsl._

    def getByIP(ip: String): F[IP] =  for {
      key <- Concurrent[F].delay(s"geoip:$ip")
      hasKey <- cmd.exists(key)
      stateUri <- Concurrent[F].delay(Uri.unsafeFromString(GeoIPApiUri.builder(GeoIPApiUri(ip))))
      resp <- if (!hasKey) {
        for {
          cdata <- C.expect[IP](GET(stateUri)).adaptError { case t =>
            L.error(s"decode error {$t.getMessage}")
            DataError(t) }
          _ <- setRedisKey(key, cdata.asJson.toString)
        } yield cdata
      } else
        getIPFromRedis(key)
    } yield resp
  }
}


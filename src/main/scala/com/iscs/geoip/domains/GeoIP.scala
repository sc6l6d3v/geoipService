package com.iscs.geoip.domains

import cats.effect.{Concurrent, ConcurrentEffect, Sync}
import cats.implicits._
import com.iscs.geoip.api.GeoIPApiUri
import com.iscs.geoip.util.{DbClient, MongoCollectionEffect}
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
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.collection.immutable.Document

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

  def impl[F[_]: Concurrent: Sync: ConcurrentEffect](C: Client[F], dbClient: DbClient[F])
                                  (implicit cmd: RedisCommands[F, String, String]): GeoIP[F] = new GeoIP[F]{
    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F]{}
    val dbfx: MongoCollectionEffect[Document] = dbClient.fxMap("ipdb")
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
          cdataJson <- Concurrent[F].delay(cdata.asJson)
          cdataStr <- Concurrent[F].delay(cdataJson.toString)
          _ <- setRedisKey(key, cdataStr)
          cdataMongoJson <- Concurrent[F].delay(cdataJson.mapObject{_.remove("ip").add("_id", Json.fromString(ip))})
          _ <- dbfx.insertOne(BsonDocument(cdataMongoJson.toString)).handleError{e =>
            L.error(s""""exception occurred inserting {}" exception="{}"""", cdataStr, e)
            None
          }
        } yield cdata
      } else
        getIPFromRedis(key)
    } yield resp
  }
}


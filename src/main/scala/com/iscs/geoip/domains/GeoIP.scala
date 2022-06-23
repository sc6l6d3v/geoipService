package com.iscs.geoip.domains

import cats.effect.Sync
import cats.implicits._
import com.iscs.geoip.api.GeoIPApiUri
import com.mongodb.client.result.InsertOneResult
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import mongo4cats.bson.Document
import mongo4cats.collection.MongoCollection
import zio.json._
import sttp.capabilities
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.model.UriInterpolator
import zio.json.ast.Json

trait GeoIP[F[_]] extends Cache[F] {
  def getByIP(state: String): F[IP]
}

object GeoIP extends UriInterpolator {
  private val L = Logger[this.type]

  def apply[F[_]](implicit ev: GeoIP[F]): GeoIP[F] = ev

  final case class DataError(e: Throwable) extends RuntimeException

  def fromState(state: String): IP = state.fromJson[IP] match {
    case Right(ip)   => ip
    case Left(_) => IP()
  }

  def impl[F[_]: Sync](coll: MongoCollection[F, Document], S: SttpBackend[F, Fs2Streams[F] with capabilities.WebSockets])
                      (implicit cmd: RedisCommands[F, String, String]): GeoIP[F] = new GeoIP[F]{
    import IP._

    L.info(s"${IP}")

    def getByIP(ip: String): F[IP] = {
        for {
          key <- Sync[F].delay(s"geoip:$ip")
          hasKey <- cmd.exists(key)
          geouri <- Sync[F].delay(GeoIPApiUri.builder(GeoIPApiUri(ip)))
          ipRequestUri <- Sync[F].delay(uri"$geouri")
          resp <- if (!hasKey) {
            for {
              cdata2 <- basicRequest.get(ipRequestUri).send(S)
                .map[Either[String, IP]] {
                  _.body.map {
                    _.fromJson[IP]
                  }.getOrElse(Right(IP()))
                }
              cdata <- Sync[F].delay(cdata2.getOrElse(IP())) /*C.expect[IP](stateUri) C.expect[IP](GET(stateUri)).adaptError { case t =>
            L.error(s"decode error {$t.getMessage}")
            DataError(t) }*/
              cdataJson <- Sync[F].delay(cdata.toJson)
              cdataJson2 <- Sync[F].delay(cdata.toJsonAST.map { js =>
                Json.Obj.decoder.fromJsonAST(js).map { jsobj =>
                  Right(jsobj)
                }.getOrElse(Left("bad obj"))
              }.getOrElse(Left("bad json")))
              cdataDoc <- Sync[F].delay(cdataJson2.map { jsonObj =>
                Right[String, Document](jsonObj.fields.foldLeft(Document.empty) { (acc, elt) =>
                  val fixStr = elt._2.toString.replaceAll(""""""", "")
                  val fixKey = if (elt._1 == "ip") "_id" else elt._1
                  acc.append(fixKey, fixStr)
                })
              }.getOrElse(Left("bad obj")))
              _ <- cdataDoc match {
                case Right(doc) =>
                  L.info(s"inserting ${doc.toJson}")
                  coll.insertOne(doc).handleError { e =>
                    L.error(s""""exception occurred inserting {}" exception="{}"""", doc, e)
                    InsertOneResult.unacknowledged()
                  }
                case Left(str) => L.error(s""""error on $str" could not save""")
                  Sync[F].unit
              }
              cdataStr <- Sync[F].delay(cdataJson)
              _ <- setRedisKey(key, cdataStr)
            } yield cdata
          } else
            getIPFromRedis(key)
        } yield resp
    }
  }
}


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
import sttp.model.{Uri, UriInterpolator}
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

    def getIpDoc(ip: Json.Obj): F[Document] = {
      for {
        doc <- Sync[F].delay(ip.fields.foldLeft(Document.empty) { (acc, elt) =>
          val fixStr = elt._2.toString.replaceAll(""""""", "")
          val fixKey = if (elt._1 == "ip") "_id" else elt._1
          acc.append(fixKey, fixStr)
        })
      } yield doc
    }

    def getIpObj(ip: IP): F[Option[Json.Obj]] = {
      for {
        ipDataObj <- Sync[F].delay(Json.Obj.decoder.fromJsonAST(
          ip.toJsonAST.getOrElse(Json.Null)
        ).getOrElse(Json.Obj())
        )
      } yield {
        if (ipDataObj.fields.size == 0)
          Option.empty[Json.Obj]
        else
          Some(ipDataObj)
      }
    }

    def getIpData(ipUri: Uri): F[Option[IP]] = {
      for {
        ipDataEither <- basicRequest.get(ipUri).send(S)
          .map[Either[String, IP]] {
            _.body.map {
              _.fromJson[IP]
            }.getOrElse(Right(IP()))
          }.handleError { e =>
          L.error(s""""request to {} failed" exception="{}"""", ipUri, e)
          Left("Request Failed")
        }
        ipMaybe <- Sync[F].delay(ipDataEither match {
          case Right(ip) => Some(ip)
          case Left(_)   => Option.empty[IP]
        })
      } yield ipMaybe
    }

    def getByIP(ip: String): F[IP] = {
        for {
          key <- Sync[F].delay(s"geoip:$ip")
          hasKey <- cmd.exists(key)
          geouri <- Sync[F].delay(GeoIPApiUri.builder(GeoIPApiUri(ip)))
          ipRequestUri <- Sync[F].delay(uri"$geouri")
          resp <- if (!hasKey) {
            for {
              ipMaybe <- getIpData(ipRequestUri)
              ipObjMaybe <- ipMaybe match {
                case Some(ip) => getIpObj(ip)
                case _        => Sync[F].delay(Option.empty[Json.Obj])
              }
              ipDoc <- ipObjMaybe match {
                case Some(ipObj) => getIpDoc(ipObj)
                case _           => Sync[F].delay(Document.empty)
              }
              added <- if (ipDoc.isEmpty) {
                Sync[F].pure(false)
              } else {
                L.info(s"inserting ${ipDoc.toJson}")
                for {
                  result <- coll.insertOne(ipDoc).handleError { e =>
                    L.error(s""""exception occurred inserting {}" exception="{}"""", ipDoc, e)
                    InsertOneResult.unacknowledged()
                  }
                  bool <- Sync[F].delay(result.wasAcknowledged)
                } yield bool
              }
              ipData <- if (added) {
                for {
                  ip <- Sync[F].delay(ipMaybe.get)
                  ipDataStr <- Sync[F].delay(ip.toJson)
                  cdataStr <- Sync[F].delay(ipDataStr)
                  _ <- setRedisKey(key, cdataStr)
                } yield ip
              } else Sync[F].pure(IP())
            } yield ipData
          } else
            getIPFromRedis(key)
        } yield resp
    }
  }
}


package com.iscs.geoip.domains

import java.util.Date
import cats.effect.Sync
import cats.implicits._
import com.iscs.geoip.api.GeoIPApiUri
import com.mongodb.client.result.InsertOneResult
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import mongo4cats.bson.Document
import mongo4cats.collection.MongoCollection
import org.mongodb.scala.bson.BsonDateTime
import sttp.capabilities
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.model.MediaType.ApplicationJson
import sttp.model.{Uri, UriInterpolator}
import zio.json._
import zio.json.ast.Json

trait GeoIP[F[_]] extends Cache[F] {
  def getByIP(state: String): F[IP]
}

object GeoIP extends UriInterpolator {
  private val L = Logger[this.type]

  def apply[F[_]](implicit ev: GeoIP[F]): GeoIP[F] = ev

  final case class DataError(e: Throwable) extends RuntimeException

  def fromState(ip: String): IP = ip.fromJson[IP] match {
    case Right(ip)   => ip
    case Left(_) => IP()
  }

  def impl[F[_]: Sync](coll: MongoCollection[F, Document], S: SttpBackend[F, Fs2Streams[F] with capabilities.WebSockets])
                      (implicit cmd: RedisCommands[F, String, String]): GeoIP[F] = new GeoIP[F]{
    import IP._

    private val fieldCreationDate = "creationDate"
    private val fieldLastModified = "lastModified"
    private val doubleFieldSize = 15

    def getIpDoc(ip: Json.Obj): F[Document] =
      for {
        doc <- Sync[F].delay(ip.fields.foldLeft(Document.empty) { (acc, elt) =>
          val fieldVal = elt._2.toString
          val fixKey = if (elt._1 == "ip") "_id" else elt._1
          val tuple = if (elt._1.endsWith("ude")) {
            JsonDecoder.double.decodeJson(fieldVal.take(doubleFieldSize)).map { eitherDbl =>
              L.info(s"attempting ${elt._1} for $fieldVal yielding $eitherDbl")
              (fixKey, eitherDbl)
            }.getOrElse((fixKey, 0.0d))
          } else {
            JsonDecoder.string.decodeJson(fieldVal).map { either =>
              (fixKey, either)
            }.getOrElse((fixKey,""))
          }
          acc.append(tuple._1, tuple._2)
        })
        timeStamp <- Sync[F].delay(BsonDateTime(new Date().getTime))
        withDate <- Sync[F].delay(doc.append(fieldCreationDate,timeStamp).append(fieldLastModified, timeStamp))
      } yield withDate

    def getIpObj(ip: IP): F[Option[Json.Obj]] = {
      for {
        ipDataObj <- Sync[F].delay(Json.Obj.decoder.fromJsonAST(
          ip.toJsonAST.getOrElse(Json.Null)
        ).getOrElse(Json.Obj())
        )
      } yield {
        if (ipDataObj.fields.isEmpty)
          Option.empty[Json.Obj]
        else
          Some(ipDataObj)
      }
    }

    def getIpData(ipUri: Uri): F[Option[IP]] = {
      for {
        responseEither <- quickRequest.contentType(ApplicationJson).get(ipUri).send(S)
        maybeIP <- Sync[F].delay {
          val headers = responseEither.headers
          L.info(s"got headers: ${headers.map(hdr => s"${hdr.name}:${hdr.value}").mkString("+")}")
          responseEither.body.fromJson[IP].map { asIp =>
            L.info(s"converted body: $asIp")
              Some(asIp)
          }.getOrElse(Option.empty[IP])
        }
      } yield maybeIP
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
              ipObjMaybe <- ipMaybe.map(ip => getIpObj(ip)).getOrElse(Sync[F].delay(Option.empty[Json.Obj]))
              ipDoc <- ipObjMaybe.map(json => getIpDoc(json)).getOrElse(Sync[F].delay(Document.empty))
              added <- if (ipDoc.isEmpty) {
                Sync[F].pure(false)
              } else {
                for {
                  _ <- Sync[F].delay(L.info(s"inserting $ipDoc"))
                  result <- coll.insertOne(ipDoc).handleError { e =>
                    L.error(s""""exception occurred inserting {}" exception="{}"""", ipDoc, e)
                    InsertOneResult.unacknowledged()
                  }
                } yield result.wasAcknowledged
              }
              ipData <- if (added) {
                for {
                  ip <- Sync[F].delay(ipMaybe.get)
                  _ <- setRedisKey(key, ip.toJson)
                } yield ip
              } else Sync[F].pure(IP())
            } yield ipData
          } else
            getIPFromRedis(key)
        } yield resp
    }
  }
}


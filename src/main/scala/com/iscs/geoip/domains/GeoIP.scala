package com.iscs.geoip.domains

//import zio._
//import zio.interop.catz._
//import cats._
import cats.effect.Sync
import cats.implicits._
import com.iscs.geoip.api.GeoIPApiUri
import com.iscs.geoip.util.DbClient
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands
import zio.json._
//import zio.json.interop.http4s._
//import org.http4s.Method._
import org.http4s.client.Client
//import org.http4s.client.dsl.Http4sClientDsl
//import org.http4s._
//import org.http4s.Status.{NotFound, Successful}
//import org.http4s.syntax.all._
import org.mongodb.scala.Document
//import org.mongodb.scala.bson.BsonDocument
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

  def impl[F[_]: Sync](C: Client[F], dbClient: DbClient[F], S: SttpBackend[F, Fs2Streams[F] with capabilities.WebSockets])
                      (implicit cmd: RedisCommands[F, String, String]): GeoIP[F] = new GeoIP[F]{
//    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F]{}
    val fdbfx = dbClient.getCollection("ipdb")
//    implicit val stateEntityDecoder: EntityDecoder[F, IP] = jsonOf[F, IP]
    import IP._

    L.info(s"${IP}")

    def getByIP(ip: String): F[IP] = {
        for {
          key <- Sync[F].delay(s"geoip:$ip")
          hasKey <- cmd.exists(key)
          geouri <- Sync[F].delay(GeoIPApiUri.builder(GeoIPApiUri(ip)))
          stateUri <- Sync[F].delay(uri"$geouri")
//          stateUri2 <- Sync[F].delay(Uri.unsafeFromString(GeoIPApiUri.builder(GeoIPApiUri(ip))))
          resp <- if (!hasKey) {
            for {
/*              cdata <- C.get(stateUri) {
                case Successful(resp) => resp.as[IP]
                case NotFound(_) => Sync[F].pure(IP())
                case default => Sync[F].pure(IP())
              }*/
              cdata2 <- basicRequest.get(stateUri).send(S)
                .map[Either[String, IP]]{_.body.map{_.fromJson[IP]}.getOrElse(Right(IP()))}
              cdata <- Sync[F].delay(cdata2.getOrElse(IP()))/*C.expect[IP](stateUri) C.expect[IP](GET(stateUri)).adaptError { case t =>
            L.error(s"decode error {$t.getMessage}")
            DataError(t) }*/
              cdataJson <- Sync[F].delay(cdata.toJson)
              cdataJson2 <- Sync[F].delay(cdata.toJsonAST.map{js =>
                Json.Obj.decoder.fromJsonAST(js).map { jsobj =>
                  Right(jsobj)
                }.getOrElse(Left("bad obj"))
              }.getOrElse(Left("bad json")))
              cdataDoc <- Sync[F].delay(cdataJson2.map { jsonObj =>
                Right(jsonObj.fields.foldLeft(Document.empty){ (acc, elt) =>
                  acc + (elt._1 -> elt._2.asInstanceOf[String])
                })
              }.getOrElse(Left("bad obj")))
              dbfx <- fdbfx
              _ <- cdataDoc match {
                case Right(doc) => dbfx.insertOne(doc)
                case Left(str)  => L.error(s""""error on $str" could not save""")
                  Sync[F].unit
              }
              cdataStr <- Sync[F].delay(cdataJson)
              _ <- setRedisKey(key, cdataStr)
//              cdataMongoJson <- Sync[F].delay(cdataJson.mapObject {
//                _.remove("ip").add("_id", Json.fromString(state))
//              })
              //            _ <- dbfx.insertOne(BsonDocument(cdataMongoJson.toString))/*.handleError{e =>
              //                        L.error(s""""exception occurred inserting {}" exception="{}"""", cdataStr, e)
              //                        None
              //                      } */  // TODO error handler
            } yield cdata
          } else
            getIPFromRedis(key)
        } yield resp
    }
  }
}


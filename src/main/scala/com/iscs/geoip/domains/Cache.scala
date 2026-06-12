package com.iscs.geoip.domains

import scala.concurrent.duration.*
import cats.effect.Sync
import cats.syntax.all.*
import com.iscs.geoip.domains.GeoIP.fromJsonIP
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands

trait Cache[F[_]] {
  private val L = Logger[this.type]
  private val cacheTtl: FiniteDuration =
    sys.env.getOrElse("REDIS_TTL_MINUTES", "15").toLong.minutes

  def getIPFromRedis[S[_]: Sync](key: String)(implicit cmd: RedisCommands[S, String, String]): S[IP] = for {
    memValOpt <- cmd.get(key)
    retrieved <- Sync[S].delay(memValOpt.map{ memVal =>
      L.info("\"retrieved key\" key={} value={}", key, memVal)
      fromJsonIP(memVal)
    }.getOrElse(IP()))
  } yield retrieved

  def setRedisKey[S[_]: Sync](key: String, inpValue: String)(
    implicit cmd: RedisCommands[S, String, String]): S[Unit] = for {
    asString <- Sync[S].delay(inpValue)
    _ <- Sync[S].delay(L.info("\"setting key\" key={} ttl={} value={}", key, cacheTtl, asString))
    _ <- cmd.setEx(key, asString, cacheTtl)
  } yield ()
}

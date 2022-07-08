package com.iscs.geoip.domains

import cats.effect.Sync
import cats.implicits._
import com.iscs.geoip.domains.GeoIP.fromState
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands

trait Cache[F[_]] {
  private val L = Logger[this.type]

  def getIPFromRedis[F[_]: Sync](key: String)(implicit cmd: RedisCommands[F, String, String]): F[IP] = for {
    memValOpt <- cmd.get(key)
    retrieved <- Sync[F].delay(memValOpt.map{ memVal =>
      L.info("\"retrieved key\" key={} value={}", key, memVal)
      fromState(memVal)
    }.getOrElse(IP()))
  } yield retrieved

  def setRedisKey[F[_]: Sync](key: String, inpValue: String)(
    implicit cmd: RedisCommands[F, String, String]): F[Unit] = for {
    asString <- Sync[F].delay(inpValue)
    _ <- Sync[F].delay(L.info("\"setting key\" key={} value={}", key, asString))
    _ <- cmd.set(key, asString)
  } yield ()
}

package com.iscs.geoip.domains

import cats.effect.Sync
import cats.implicits._
import com.iscs.geoip.domains.GeoIP.fromState
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands

trait Cache[F[_]] {
  private val L = Logger[this.type]

  def getIPFromRedis[S[_]: Sync](key: String)(implicit cmd: RedisCommands[S, String, String]): S[IP] = for {
    memValOpt <- cmd.get(key)
    retrieved <- Sync[S].delay(memValOpt.map{ memVal =>
      L.info("\"retrieved key\" key={} value={}", key, memVal)
      fromState(memVal)
    }.getOrElse(IP()))
  } yield retrieved

  def setRedisKey[S[_]: Sync](key: String, inpValue: String)(
    implicit cmd: RedisCommands[S, String, String]): S[Unit] = for {
    asString <- Sync[S].delay(inpValue)
    _ <- Sync[S].delay(L.info("\"setting key\" key={} value={}", key, asString))
    _ <- cmd.set(key, asString)
  } yield ()
}

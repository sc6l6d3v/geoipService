package com.iscs.covidbystates.domains

import cats.effect.{Concurrent, Sync}
import cats.implicits._
import com.iscs.covidbystates.domains.GeoIP.{IP, fromState}
import com.typesafe.scalalogging.Logger
import dev.profunktor.redis4cats.RedisCommands

trait Cache[F[_]] {
  private val L = Logger[this.type]

  def getIPFromRedis[F[_]: Concurrent: Sync](key: String)(implicit cmd: RedisCommands[F, String, String]): F[IP] = for {
    memValOpt <- cmd.get(key)
    retrieved <- Concurrent[F].delay(memValOpt.map{ memVal =>
      L.info("\"retrieved key\" key={} value={}", key, memVal)
      fromState(memVal)
    }.getOrElse(IP.empty))
  } yield retrieved

  def setRedisKey[F[_]: Concurrent: Sync](key: String, inpValue: String)(implicit cmd: RedisCommands[F, String, String]): F[Unit] = for {
    asString <- Concurrent[F].delay(inpValue)
    _ <- Concurrent[F].delay(L.info("\"setting key\" key={} value={}", key, asString))
    _ <- cmd.set(key, asString)
  } yield ()
}

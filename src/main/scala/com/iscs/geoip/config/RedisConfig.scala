package com.iscs.geoip.config

import cats.effect.Resource
import cats.effect.kernel.Sync
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.connection._
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.MkRedis

class RedisConfig[F[_]: MkRedis : Sync]() {
  private val redisHost = sys.env.getOrElse("REDISHOST", "localhost")
  private val pwd = sys.env.getOrElse("REDISKEY", "NOREDISKEY") //.replace("@", "%40")
  private val JUri2 = s"redis://$pwd@$redisHost"
  private val stringCodec: RedisCodec[String, String] = RedisCodec.Utf8
  val resource2 =   for {
       uri <- Resource.eval(RedisURI.make[F](JUri2))
       cli <- RedisClient[F].fromUri(uri)
       rd <- Redis[F].fromClient(cli, stringCodec)
     } yield rd
}

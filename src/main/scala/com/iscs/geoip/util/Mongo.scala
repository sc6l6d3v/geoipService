package com.iscs.geoip.util

import com.iscs.geoip.config.MongodbConfig

object Mongo {
  def fromUrl(): MongodbConfig = {
    val host = sys.env.getOrElse("MONGOURI", "localhost")
    val isReadOnly = sys.env.getOrElse("MONGORO", "false").toBoolean
    MongodbConfig(host, isReadOnly)
  }
}

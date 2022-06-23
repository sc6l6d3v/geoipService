package com.iscs.geoip.config

import com.mongodb.ReadPreference
import org.mongodb.scala.{ConnectionString, MongoClientSettings, MongoCredential}

case class MongodbConfig(url: String, isReadOnly: Boolean = false) {
  val connection = new ConnectionString(url)

  val credentials: MongoCredential = connection.getCredential

  val useSSL = connection.getSslEnabled != null

  val isReplicaSet = connection.getRequiredReplicaSetName != null

  val baseSettings = MongoClientSettings.builder()
    .applyToConnectionPoolSettings(b => b.minSize(128).maxSize(256))
    .applyConnectionString(connection)
    .readPreference(ReadPreference.primaryPreferred)

  val withCredentials = if (credentials == null) baseSettings else baseSettings.credential(credentials)

  val settings: MongoClientSettings = if (useSSL)
    withCredentials
      .applyToSslSettings(b => b.enabled(useSSL))
      .build()
  else
    withCredentials
      .build()
}

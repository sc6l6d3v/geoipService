package com.iscs.covidbystates.util

import cats.effect.{ConcurrentEffect, Effect}
import com.iscs.covidbystates.config.MongodbConfig
import org.mongodb.scala.bson.Document
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

case class DbClient[F[_]: Effect](config: MongodbConfig, dbName: String, collNames: List[String])(implicit E: Effect[F]) extends AutoCloseable {
  private val client = MongoClient(config.settings)

  val db: MongoDatabase = client.getDatabase(dbName)

  def dbFX(collection: MongoCollection[Document]) = new MongoCollectionEffect[Document](collection)

  override def close(): Unit = client.close()
}

object DbClient {
  def apply[F[_]](client: MongodbConfig, names: List[String])
                             (implicit E: ConcurrentEffect[F]): F[DbClient[F]] =
    E.delay(DbClient(client, names.head, names.tail))
}

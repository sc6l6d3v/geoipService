package com.iscs.geoip.util

import cats.effect.{Async, Resource, Sync}
import cats.syntax.all._
import com.iscs.geoip.config.MongodbConfig
import com.typesafe.scalalogging.Logger
import mongo4cats.bson.Document
import mongo4cats.client.MongoClient
import mongo4cats.collection.MongoCollection
import mongo4cats.database.MongoDatabase

class DbClient[F[_]](val dbName: String,
                     val collNames: List[String],
                     val mongoCfg: MongodbConfig)(
  implicit F: Sync[F], A: Async[F]) {
  private val L = Logger[this.type]

  val dbResource: Resource[F, MongoClient[F]] = MongoClient.fromConnectionString(mongoCfg.url)

  def getFxMap(db: F[MongoDatabase[F]]): F[Map[String, F[MongoCollection[F, Document]]]] = for {
    mongoDb <- db
    mongoMaps <- Sync[F].delay(collNames.foldLeft(Map.empty[String, F[MongoCollection[F, Document]]]) { case (acc, elt) =>
      acc ++ Map(elt -> mongoDb.getCollection(elt))
    })
  } yield mongoMaps

  def getCollection(db: F[MongoDatabase[F]], collName: String): F[MongoCollection[F, Document]] = for {
    fxMap <- getFxMap(db)
    maybeName <- Sync[F].delay(if (fxMap.contains(collName)) collName else "DONTUSE")
    coll <- fxMap(maybeName)
  } yield coll
}


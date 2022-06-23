package com.iscs.geoip.util

import cats.effect.{Async, Resource, Sync}
import com.iscs.geoip.config.MongodbConfig
import com.typesafe.scalalogging.Logger
import cats.syntax.all._
import mongo4cats.client.{MongoClient, MongoDriverInformation}
import mongo4cats.collection.MongoCollection
import mongo4cats.collection.operations.Index
import mongo4cats.database.MongoDatabase
import com.typesafe.scalalogging.Logger
import mongo4cats.bson.Document

class DbClient[F[_]](//client: MongoClient[F],
                     val dbName: String,
                     val collNames: List[String],
                     val mongoCfg: MongodbConfig)(
  implicit F: Sync[F], A: Async[F]
) {//dbName: String, collNames: List[String])(implicit E: Effect[F]) extends AutoCloseable {
  private val L = Logger[this.type]

  val mongoDriveInfo: MongoDriverInformation = MongoDriverInformation.builder
    .driverName("mongo-scala-driver")
    .driverVersion("")
    .driverPlatform("Scala 2.13.6")
    .build

  val dbResource2: Resource[F, MongoClient[F]] = MongoClient.create[F](mongoCfg.settings, mongoDriveInfo)

  val dbResource = MongoClient.fromConnectionString(mongoCfg.url)

  val xxxxx = dbResource.use ( client => for {
    db <- client.getDatabase(dbName)
    collMap <-  Sync[F].delay(collNames.foldLeft(Map.empty[String, F[MongoCollection[F, Document]]]) { case (acc, elt) =>
      acc ++ Map(elt -> db.getCollection(elt))
    } )
  } yield (client, db, collMap)
  )

  val db: F[MongoDatabase[F]] = dbResource.use(_.getDatabase(dbName))

  def getFxMap: F[Map[String, F[MongoCollection[F, Document]]]] = for {
    mongoDb <- db
    mongoMaps <- Sync[F].delay(collNames.foldLeft(Map.empty[String, F[MongoCollection[F, Document]]]) { case (acc, elt) =>
      acc ++ Map(elt -> mongoDb.getCollection(elt))
    })
  } yield mongoMaps

  def getCollection(collName: String): F[MongoCollection[F, Document]] = for {
    fxMap <- getFxMap
    maybeName <- Sync[F].delay(if (fxMap.contains(collName)) collName else "DONTUSE")
    coll <- fxMap(maybeName)
  } yield coll
}


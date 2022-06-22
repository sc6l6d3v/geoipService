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
                     dbName: String,
                     collNames: List[String],
                     mongoCfg: MongodbConfig)(
  implicit F: Sync[F], A: Async[F]
) {//dbName: String, collNames: List[String])(implicit E: Effect[F]) extends AutoCloseable {
  private val L = Logger[this.type]

  val mongoDriveInfo: MongoDriverInformation = MongoDriverInformation.builder
    .driverName("mongo-scala-driver")
    .driverVersion("")
    .driverPlatform("Scala 2.13.6")
    .build

  val dbResource: Resource[F, MongoClient[F]] = MongoClient.create[F](mongoCfg.settings, mongoDriveInfo)

  def db: F[MongoDatabase[F]] = for {
    db <- dbResource.use(client2 => client2.getDatabase(dbName))
  } yield db

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

/*object DbClient {
  def apply[F[_]](client: MongodbConfig, names: List[String]): F[DbClient[F]] =
    E.delay(DbClient(client, names.head, names.tail))
}*/

//class DbClient[F[_]](client: MongodbConfig, names: List[String])

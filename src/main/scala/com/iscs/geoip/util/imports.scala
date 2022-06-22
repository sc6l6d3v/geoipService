package com.iscs.geoip.util

import cats.implicits._
import cats.effect.implicits._
import com.mongodb.client.model.WriteModel
import com.mongodb.client.result.UpdateResult
import com.typesafe.scalalogging.Logger
import fs2._
import mongo4cats.collection.MongoCollection
import mongo4cats.collection.{BulkWriteOptions, WriteCommand}
import org.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDocument, BsonNumber, BsonString}
import org.mongodb.scala.result.{DeleteResult, InsertManyResult, InsertOneResult}
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.{IndexModel, UpdateOptions}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{BulkWriteResult, bson}

import scala.reflect.ClassTag

/*object imports {
  final val Mongo = com.iscs.geoip.util.Mongo

  def toAsync[F[_]: ConcurrentEffect, T](obs: Observable[T]): F[Option[T]] =
    ConcurrentEffect[F].async[Option[T]] { (cb: Either[Throwable, Option[T]] => Unit) =>
      obs.subscribe(
        (result: T) => cb(Right(result.some)),
        (ex: Throwable) => cb(Left(ex)),
        () => cb(Option.empty[T].asRight))
    }

  def toStream[F[_]: ConcurrentEffect, T](observ:Observable[T]): Stream[F, T] =
    for {
      que <- Stream.eval(Queue.noneTerminated[F,Either[Throwable, T]])
      _ <- Stream.eval{
        ConcurrentEffect[F].delay(observ.subscribe(
          (next: T) =>
            que.enqueue1(Right(next).some)
              .runAsync(_ => IO.unit).unsafeRunSync(),
          (ex: Throwable) =>
            que.enqueue1(Left(ex).some)
              .runAsync(_ => IO.unit).unsafeRunSync(),
          () =>
            que.enqueue1(Option.empty[Either[Throwable, T]])
              .runAsync(_ => IO.unit).unsafeRunSync()
        ))
      }
      row <- que.dequeue.rethrow
    } yield row
}*/

class MongoCollectionEffect[F[_]](underlying: MongoCollection[F, Document]) {
  private val L = Logger[this.type]

  private val collName = underlying.namespace.getCollectionName
  def getCollName: String = collName

  private val batchsize = 1000

  def getProjectionFields(projSet: Map[String, Boolean]): List[Bson] =
    projSet.map{ case(field, action) =>
      if (action) include(field) else exclude(field)
    }.toList

  def getProjections(projSet: List[Bson]): Bson = fields(projSet:_*)

  def getCompareFilter(compareOp: String, fieldName: String, compVal: Boolean): Bson =
    bson.BsonDocument(List((s"$$$compareOp", BsonArray(BsonString(s"$$$fieldName"), BsonBoolean(compVal)))))

  def getCompareFilter(compareOp: String, fieldName: String, compVal: String): Bson =
    bson.BsonDocument(List((s"$$$compareOp", BsonArray(BsonString(s"$$$fieldName"), BsonString(compVal)))))

  def getCompareFilter(compareOp: String, fieldName: String, compVal: Double): Bson =
    bson.BsonDocument(List((s"$$$compareOp", BsonArray(BsonString(s"$$$fieldName"), BsonNumber(compVal)))))

  def getCompareFilter(compareOp: String, fieldName: String, compVal: Int): Bson =
    bson.BsonDocument(List((s"$$$compareOp", BsonArray(BsonString(s"$$$fieldName"), BsonNumber(compVal)))))

  def getCondFilter(fieldName: String, item: String, cond: Bson): Bson = {
    val inputAsCond = new BsonDocument()
      .append("input", BsonString(s"$$$fieldName"))
      .append("as", BsonString(s"$item"))
      .append("cond", cond.toBsonDocument)
    val filterBson = new BsonDocument("$filter", inputAsCond)
    computed(fieldName, filterBson)
  }

  def aggregate(stages: Seq[Bson]): Stream[F, Document] = underlying.aggregate(stages).stream

  def bulkWrite(requests: Seq[WriteCommand[Document]]): F[BulkWriteResult] =
    underlying.bulkWrite(requests, BulkWriteOptions(ordered = false))

  def createIndexes(indexes: List[IndexModel]): Unit = {
    val combinedIndexes = indexes.map { ndx => ndx.getKeys}
    //    val combinedIndexes = BsonDocument.apply(indexes.map { ndx => ndx.getKeys}.map{_.toBsonDocument }: _*)
    /*    underlying.createIndex(combinedIndexes)
            .subscribe((i: String) =>
              L.info(s"${collName}_index_info=$i"), (e: Throwable) => L.error("Exception occurred while creating index", e), () => L.info("Indexes creation completed")) */
  }

  def distinct(key: String): Stream[F, Document] = underlying.distinct(key).stream

  def findOne(filter: Bson): F[Option[Document]] = underlying.find(filter).limit(1).first

  def find(filter: Bson): Stream[F,Document] = underlying.find(filter).stream //.batchSize(batchsize)

  def find(filter: Bson, projections: Map[String, Boolean]): Stream[F,Document] =
    underlying.find(filter) //.batchSize(batchsize)
      .projection(fields(getProjectionFields(projections):_*)).stream

  def find(filter: Bson, limit: Int, offset: Int): Stream[F,Document] =
    underlying.find(filter).skip(offset).limit(limit).stream

  def find(filter: Bson, limit: Int, offset: Int, projections: Map[String, Boolean]): Stream[F,Document] =
    underlying.find(filter)
      .projection(fields(getProjectionFields(projections): _*))
      .skip(offset)
      .limit(limit)
      .stream

  def find(filter: Bson, limit: Int, offset: Int, projections: Map[String, Boolean], sortFields: Bson): Stream[F,Document] =
    underlying.find(filter)
      .projection(fields(getProjectionFields(projections):_*))
      .skip(offset)
      .limit(limit)
      .sort(sortFields)
      .stream

  def count: F[Long] = underlying.count //.countDocuments()

  def count(filter: Bson): F[Long] = underlying.count(filter)

  def insertOne(document: Document): F[InsertOneResult] = underlying.insertOne(document)

  def insertMany(documents: Seq[Document]): F[InsertManyResult] = underlying.insertMany(documents)

  def removeOne(filter: Bson): F[DeleteResult] = underlying.deleteOne(filter)

  def removeMany(filter: Bson): F[DeleteResult] = underlying.deleteMany(filter)

  def updateOne(filter: Bson, update: Bson): F[UpdateResult] = underlying.updateOne(filter, update)

  def updateOne(filter: Bson, update: Bson, options: UpdateOptions): F[UpdateResult] =
    underlying.updateOne(filter, update, options)

  def updateMany(filter: Bson, update: Bson): F[UpdateResult] = underlying.updateMany(filter, update)
}


/*
class MongoCollectionEffect2[A](val underlying: MongoCollection[A])(implicit c: ClassTag[A]) {
  private val L = Logger[this.type]

  private val collName = underlying.namespace.getCollectionName
  def getCollName: String = collName
  private val batchsize = 1000

  import imports._

  def bulkWrite[F[_]: ConcurrentEffect](requests: Seq[WriteModel[A]]): F[Option[BulkWriteResult]] =
    toAsync(underlying.bulkWrite(requests))

  def distinct[F[_]: ConcurrentEffect](key: String): Stream[F,A] = toStream(underlying.distinct(key))

  def findOne[F[_]: ConcurrentEffect](filter: Bson): F[Option[A]] = toAsync(underlying.find(filter)
    .limit(1).first())

  def find[F[_]: ConcurrentEffect](filter: Bson): Stream[F,A] =
    toStream(underlying.find(filter).batchSize(batchsize))

  def find[F[_]: ConcurrentEffect](filter: Bson, limit: Int, offset: Int): Stream[F,A] =
    toStream(underlying.find(filter).skip(offset).limit(limit))

  def count[F[_]: ConcurrentEffect]: F[Option[Long]] = toAsync(underlying.countDocuments())

  def count[F[_]: ConcurrentEffect](filter: Bson): F[Option[Long]] = toAsync(underlying.countDocuments(filter))

  def insertOne[F[_]: ConcurrentEffect](document: A): F[Option[InsertOneResult]] =
    toAsync(underlying.insertOne(document))

  def insertMany[F[_]: ConcurrentEffect](documents: Seq[A]): F[Option[InsertManyResult]] =
    toAsync(underlying.insertMany(documents))

  def removeOne[F[_]: ConcurrentEffect](filter: Bson): F[Option[DeleteResult]] =
    toAsync(underlying.deleteOne(filter))

  def removeMany[F[_]: ConcurrentEffect](filter: Bson): F[Option[DeleteResult]] =
    toAsync(underlying.deleteMany(filter))

  def updateOne[F[_]: ConcurrentEffect](filter: Bson, update: Bson): F[Option[UpdateResult]] =
    toAsync(underlying.updateOne(filter, update))

  def updateMany[F[_]: ConcurrentEffect](filter: Bson, update: Bson): F[Option[UpdateResult]] =
    toAsync(underlying.updateMany(filter, update))
}
*/


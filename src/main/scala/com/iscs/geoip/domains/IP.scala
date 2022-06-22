package com.iscs.geoip.domains

//import cats.Applicative
//import cats.implicits._
//import cats.effect._
//import zio._
//import zio.interop.catz._
import zio.json._
//import zio.json.interop.http4s._
//import org.http4s.EntityDecoder

final case class IP(ip: String = "", country_code2: String = "", country_code3: String = "", country_name: String = "",
                    state_prov: String = "", district: String = "", city: String = "", zipcode: String = "",
                    latitude: String = "", longitude: String = "", organization: String = "") {
  override def toString: String = s"$this"
}

object IP {
  implicit val ipDecoder: JsonDecoder[IP] = DeriveJsonDecoder.gen[IP]
  implicit val ipEncoder: JsonEncoder[IP] = DeriveJsonEncoder.gen[IP]
/*  implicit def stateEntityDecoder[F[_]: Applicative]: EntityDecoder[F, IP] = {
    val zzz = ZIO.runtime
    ZIO.runtime.flatMap { implicit r: Runtime[Any] =>
      val F: cats.effect.Concurrent[Task] = implicitly
      F.racePair(F.unit, F.unit)
    }
    jsonOf[F, IP]
  }*/
}

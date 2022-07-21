package com.iscs.geoip.domains

import zio.json._
import scala.util.Try

final case class IP(ip: String = "", country_code2: String = "", country_code3: String = "", country_name: String = "",
                    state_prov: String = "", district: String = "", city: String = "", zipcode: String = "",
                    latitude: String = "", longitude: String = "", organization: String = "")

object IP {
  implicit val ipDecoder: JsonDecoder[IP] = DeriveJsonDecoder.gen[IP]
  implicit val ipEncoder: JsonEncoder[IP] =
    JsonEncoder[IPinternal]
    .contramap(ip =>
    IPinternal(ip.ip, ip.country_code2, ip.country_code3, ip.country_name, ip.state_prov, ip.district, ip.city, ip.zipcode,
      Try(ip.latitude.toDouble).toOption.getOrElse(0.0D),
      Try(ip.longitude.toDouble).toOption.getOrElse(0.0D),
      ip.organization
    ))
}

package com.iscs.geoip.domains

import zio.json._

final case class IP(ip: String = "", country_code2: String = "", country_code3: String = "", country_name: String = "",
                    state_prov: String = "", district: String = "", city: String = "", zipcode: String = "",
                    latitude: String = "", longitude: String = "", organization: String = "") {
  override def toString: String = s"$this"
}

object IP {
  implicit val ipDecoder: JsonDecoder[IP] = DeriveJsonDecoder.gen[IP]
  implicit val ipEncoder: JsonEncoder[IP] = DeriveJsonEncoder.gen[IP]
}

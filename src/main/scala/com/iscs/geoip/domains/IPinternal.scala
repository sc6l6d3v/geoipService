package com.iscs.geoip.domains

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class IPinternal(ip: String = "", country_code2: String = "", country_code3: String = "", country_name: String = "",
                            state_prov: String = "", district: String = "", city: String = "", zipcode: String = "",
                            latitude: Double = 0.0d, longitude: Double = 0.0d, organization: String = "") {
  override def toString: String = s"$this"
}

object IPinternal {
  implicit val ipIntDecoder: JsonDecoder[IPinternal] = DeriveJsonDecoder.gen[IPinternal]
  implicit val ipIntEncoder: JsonEncoder[IPinternal] = DeriveJsonEncoder.gen[IPinternal]
}



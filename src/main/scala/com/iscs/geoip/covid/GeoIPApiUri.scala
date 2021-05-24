package com.iscs.geoip.covid

case class GeoIPApiUri(base: String, path: String, ip: String = "127.0.0.1") {
  def withBase(newBase: String): GeoIPApiUri = copy(base = newBase)

  def withPath(newPath: String): GeoIPApiUri = copy(path = newPath)
}

object GeoIPApiUri {
  val base = "https://api.ipgeolocation.io"
  val path = "/ipgeo"
  val keyParam = "apiKey"
  val keyValue = sys.env.getOrElse("GEOIPKEY", "NOKEY")
  val fieldsParam = "fields"
  val fieldsValues = "geo,organization"

  def apply(ip: String): GeoIPApiUri = {
    GeoIPApiUri(base, path, ip)
  }

  def builder(uri: GeoIPApiUri): String = s"${uri.base}${uri.path}?$keyParam=$keyValue&ip=${uri.ip}&$fieldsParam=$fieldsValues"
}

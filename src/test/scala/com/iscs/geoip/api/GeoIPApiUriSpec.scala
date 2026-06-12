package com.iscs.geoip.api

import org.specs2.mutable.Specification

class GeoIPApiUriSpec extends Specification {

  private val base = "https://api.ipgeolocation.io"
  private val path = "/ipgeo"
  private val keyParam = "apiKey"
  private val keyValue = sys.env.getOrElse("GEOIPKEY", "NOKEY")
  private val fullBase = s"$base$path?$keyParam=$keyValue"
  private val fieldsParam = "fields"
  private val fieldsValues = "geo,organization"

  "GeoIPApiUri" >> {
    "base checks" >> {
      GeoIPApiUri.builder(GeoIPApiUri("")) === s"$fullBase&ip=&$fieldsParam=$fieldsValues"
    }
    "withQuery override" >> {
      GeoIPApiUri.builder(GeoIPApiUri("211.40.129.246")) === s"$fullBase&ip=211.40.129.246&$fieldsParam=$fieldsValues"
    }
  }

}

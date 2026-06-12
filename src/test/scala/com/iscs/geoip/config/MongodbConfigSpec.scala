package com.iscs.geoip.config

import com.mongodb.ReadPreference
import org.specs2.mutable.Specification

class MongodbConfigSpec extends Specification {
  private val mdbConfig = MongodbConfig("mongodb://localhost:27017/crm")

  "MongodbConfig" should {
    "take a URL string parameter" in {
      mdbConfig.productElementName(0) must beEqualTo("url")
    }
    "have default false isReadOnly param" in {
      mdbConfig.productElement(1) must beEqualTo(false)
    }
    "provide a connection from Url" in {
      mdbConfig.connection.isSrvProtocol must beFalse
    }
    "may have an empty credentials" in {
      mdbConfig.credentials must beNull
    }
    "may have SSL disabled" in {
      mdbConfig.useSSL must beFalse
    }
    "may not be a replicaSet" in {
      mdbConfig.isReplicaSet must beFalse
    }
    "may have a builder" in {
      mdbConfig.settings.getReadPreference must beEqualTo(ReadPreference.primaryPreferred())
    }
  }
}

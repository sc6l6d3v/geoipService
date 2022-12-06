package com.iscs.geoip.config

import com.mongodb.ReadPreference
import org.specs2.mutable.Specification
import weaver.SimpleIOSuite
import weaver.specs2compat.IOMatchers

class MongodbConfigSpec extends Specification {
  private val mdbConfig = MongodbConfig("mongodb://localhost:27017/crm")

  "MongodbConfig" should {
    "take a URL string parameter" in {
      mdbConfig.productElementName(0) mustEqual "url"
    }
    "have default false isReadOnly param" in {
      mdbConfig.productElement(1) mustEqual false
    }
    "provide a connection from Url" in {
      mdbConfig.connection.isSrvProtocol mustEqual false
    }
    "may have an empty credentials" in {
      mdbConfig.credentials mustEqual null
    }
    "may have SSL disabled" in {
      mdbConfig.useSSL mustEqual false
    }
    "may not be a replicaSet" in {
      mdbConfig.isReplicaSet mustEqual false
    }
    "may have a builder" in {
      mdbConfig.settings.getReadPreference mustEqual ReadPreference.primaryPreferred()
    }
  }
}

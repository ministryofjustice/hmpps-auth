package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule

class TokenVerificationMockServer extends WireMockRule {

  TokenVerificationMockServer() {
    super(WireMockConfiguration.wireMockConfig().port(8100).usingFilesUnderClasspath("tokenverification"))
  }
}

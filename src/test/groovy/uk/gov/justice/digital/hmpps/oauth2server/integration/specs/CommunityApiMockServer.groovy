package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import com.github.tomakehurst.wiremock.junit.WireMockRule

class CommunityApiMockServer extends WireMockRule {

  CommunityApiMockServer() {
    super(8099)
  }
}

package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.junit.WireMockRule

class CommunityApiMockServer extends WireMockRule {

  CommunityApiMockServer() {
    super(WireMockConfiguration.wireMockConfig()
        .port(8099)
        .usingFilesUnderClasspath("delius")
        .extensions(new ResponseTemplateTransformer(false)))
  }
}

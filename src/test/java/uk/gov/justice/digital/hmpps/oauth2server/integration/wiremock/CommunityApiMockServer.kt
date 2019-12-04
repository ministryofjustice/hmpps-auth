package uk.gov.justice.digital.hmpps.oauth2server.integration.wiremock

import com.github.tomakehurst.wiremock.junit.WireMockRule

open class CommunityApiMockServer(port: Int = 8099) : WireMockRule(port)

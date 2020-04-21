package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class TokenVerificationExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val tokenVerificationApi = TokenVerificationMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    tokenVerificationApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    tokenVerificationApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    tokenVerificationApi.stop()
  }
}

class TokenVerificationMockServer : WireMockServer(wireMockConfig().port(8100).usingFilesUnderClasspath("tokenverification"))

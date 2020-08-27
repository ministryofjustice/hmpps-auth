package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class AzureOIDCExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val azureOIDC = AzureOIDCMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    azureOIDC.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    azureOIDC.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    azureOIDC.stop()
  }
}

class AzureOIDCMockServer : WireMockServer(
    wireMockConfig()
        .port(8101)
        .usingFilesUnderClasspath("azureoidc")
        .extensions(ResponseTemplateTransformer(false), TokenSignerTransformer()))

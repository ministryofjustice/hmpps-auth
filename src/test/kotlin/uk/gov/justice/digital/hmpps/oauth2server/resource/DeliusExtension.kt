package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor

class DeliusExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback, TestInstancePostProcessor {
  companion object {
    @JvmField
    val communityApi = CommunityApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    communityApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    communityApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    communityApi.stop()
  }

  override fun postProcessTestInstance(testInstance: Any?, context: ExtensionContext?) {
    try {
      val field = testInstance?.javaClass?.getField("communityApi")
      field?.set(testInstance, communityApi)
    } catch (e: NoSuchFieldException) {
    }
  }
}

class CommunityApiMockServer : WireMockServer(
  wireMockConfig()
    .port(8099)
    .usingFilesUnderClasspath("delius")
    .extensions(ResponseTemplateTransformer(false))
)

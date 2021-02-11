package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class RemoteClientExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val remoteClient = RemoteClientMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    remoteClient.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    remoteClient.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    remoteClient.stop()
  }
}

class RemoteClientMockServer : WireMockServer(wireMockConfig().port(port).usingFilesUnderClasspath("remoteClient")) {
  companion object {
    const val port = 8081
    const val clientBaseUrl = "http://localhost:$port/login"
  }
}

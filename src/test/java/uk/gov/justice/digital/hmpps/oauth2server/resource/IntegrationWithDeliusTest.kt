package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import uk.gov.justice.digital.hmpps.oauth2server.integration.CommunityApiMockServer

abstract class IntegrationWithDeliusTest : IntegrationTest() {
  @BeforeEach
  fun resetStubs() {
    communityApi.resetAll()
  }

  companion object {
    @JvmField
    internal val communityApi = CommunityApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      communityApi.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      communityApi.stop()
    }
  }
}

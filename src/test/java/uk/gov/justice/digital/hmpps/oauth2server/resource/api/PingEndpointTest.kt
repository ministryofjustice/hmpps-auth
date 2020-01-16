package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PingEndpointTest {
  @Test
  fun ping() {
    assertThat(PingEndpoint().ping()).isEqualTo("pong")
  }
}

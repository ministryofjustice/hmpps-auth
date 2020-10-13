package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.v3.parser.converter.SwaggerConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class SwaggerValidator : IntegrationTest() {
  @Test
  fun `validate swagger json`() {
    val result = SwaggerConverter().readLocation("$baseUrl/v3/api-docs", null, null)
    assertThat(result.messages).isEmpty()
  }
}

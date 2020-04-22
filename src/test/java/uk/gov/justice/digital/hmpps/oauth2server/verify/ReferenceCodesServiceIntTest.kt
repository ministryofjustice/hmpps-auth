package uk.gov.justice.digital.hmpps.oauth2server.verify

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
open class ReferenceCodesServiceIntTest {
  @Autowired
  private lateinit var referenceCodesService: ReferenceCodesService

  @Test
  fun isValidEmailDomain_probation() {
    assertThat(referenceCodesService.isValidEmailDomain("hmiprobation.gov.uk")).isTrue()
  }

  @Test
  fun isValidEmailDomain_invalid() {
    assertThat(referenceCodesService.isValidEmailDomain("george.gov.uk")).isFalse()
  }

  @Test
  fun isValidEmailDomain_wildcard() {
    assertThat(referenceCodesService.isValidEmailDomain("digital.justice.gov.uk")).isTrue()
  }

  @Test
  fun isValidEmailDomain_blank() {
    assertThat(referenceCodesService.isValidEmailDomain("  ")).isFalse()
  }
}

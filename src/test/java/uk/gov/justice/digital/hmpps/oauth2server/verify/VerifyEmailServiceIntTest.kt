package uk.gov.justice.digital.hmpps.oauth2server.verify

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional(transactionManager = "authTransactionManager")
open class VerifyEmailServiceIntTest {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate
  private val verifyEmailService = VerifyEmailService(null, null, jdbcTemplate, null, null, null, "templateId")


  @Test
  fun existingEmailAddresses() {
      val emails = verifyEmailService.getExistingEmailAddresses("RO_USER")
      assertThat(emails).containsExactlyInAnyOrder("phillips@bobjustice.gov.uk", "phillips@fredjustice.gov.uk")
    }

  @Test
  fun existingEmailAddresses_NotFound() {
      val emails = verifyEmailService.getExistingEmailAddresses("CA_USER")
      assertThat(emails).isEmpty()
    }
}

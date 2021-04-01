package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(transactionManager = "authTransactionManager")
class ClientRepositoryTest {
  @Autowired
  private lateinit var repository: ClientRepository

  @Test
  fun findAllByEnabledTrueOrderByName() {
    assertThat(repository.findByIdStartsWithOrderById("rotation-test-client"))
      .extracting<String> { it.id }
      .contains("rotation-test-client", "rotation-test-client-2")
  }

  @Test
  fun testMapping() {
    val client = repository.findById("deliusnewtech").orElseThrow()
    assertThat(client.id).isEqualTo("deliusnewtech")
    assertThat(client.accessTokenValidity).isEqualTo(3600)
    assertThat(client.authorities).containsExactly("SYSTEM_READ_ONLY")
    assertThat(client.authorizedGrantTypes).containsExactly("client_credentials")
    assertThat(client.scope).containsExactly("reporting")
    assertThat(client.mfa).isEqualTo(MfaAccess.untrusted)
  }
}

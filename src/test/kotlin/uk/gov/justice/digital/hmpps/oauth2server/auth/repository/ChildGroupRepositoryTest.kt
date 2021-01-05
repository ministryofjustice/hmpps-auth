package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional("authTransactionManager")
class ChildGroupRepositoryTest {

  @Autowired
  private lateinit var repository: ChildGroupRepository

  @Test
  fun givenAnExistingRoleTheyCanBeRetrieved() {
    val retrievedEntity = repository.findByGroupCode("CHILD_2").orElseThrow()
    Assertions.assertThat(retrievedEntity.groupCode).isEqualTo("CHILD_2")
    Assertions.assertThat(retrievedEntity.groupName).isEqualTo("Child - Site 2 - Group 1")
  }
}

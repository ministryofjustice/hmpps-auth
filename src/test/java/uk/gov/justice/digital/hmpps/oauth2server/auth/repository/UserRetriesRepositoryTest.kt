package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(transactionManager = "authTransactionManager")
class UserRetriesRepositoryTest {
  @Autowired
  private lateinit var repository: UserRetriesRepository

  @Test
  fun givenATransientEntityItCanBePersisted() {
    val transientEntity = transientEntity()
    val entity = UserRetries(transientEntity.username!!, transientEntity.retryCount)
    val (username) = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(username).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findById(entity.username!!).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity.username).isEqualTo(transientEntity.username)
    assertThat(retrievedEntity.retryCount).isEqualTo(transientEntity.retryCount)
  }

  @Test
  fun givenAnExistingUserTheyCanBeRetrieved() {
    val retrievedEntity = repository.findById("LOCKED_USER").orElseThrow()
    assertThat(retrievedEntity.username).isEqualTo("LOCKED_USER")
    assertThat(retrievedEntity.retryCount).isEqualTo(5)
  }

  private fun transientEntity() = UserRetries("TEST_USER", 5)
}

@file:Suppress("UsePropertyAccessSyntax")

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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.OauthCode
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(transactionManager = "authTransactionManager")
class OauthCodeRepositoryTest {
  @Autowired
  private lateinit var repository: OauthCodeRepository

  @Test
  fun givenATransientEntityItCanBePersisted() {
    val transientEntity = transientEntity("somecode")
    val entity = transientEntity("somecode")
    val persistedEntity = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.code).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findById(entity.code).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity.code).isEqualTo(transientEntity.code)
    assertThat(String(retrievedEntity.authentication!!)).isEqualTo("somevalue")
  }

  @Test
  fun deleteByCreatedDateBefore() {
    val now = LocalDateTime.now()
    val oldEntity = transientEntity("old", now.minusDays(1).minusMinutes(1))
    val newEntity = transientEntity("new", now.minusDays(1).plusMinutes(1))

    repository.save(oldEntity)
    repository.save(newEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    repository.deleteByCreatedDateBefore(now.minusDays(1))

    assertThat(repository.findById("old")).isNotPresent()
    assertThat(repository.findById("new")).get().isEqualTo(newEntity)
  }

  private fun transientEntity(code: String, createdDate: LocalDateTime = LocalDateTime.now()): OauthCode {
    val authCode = OauthCode(code)
    authCode.authentication = "somevalue".toByteArray()
    authCode.createdDate = createdDate
    return authCode
  }
}

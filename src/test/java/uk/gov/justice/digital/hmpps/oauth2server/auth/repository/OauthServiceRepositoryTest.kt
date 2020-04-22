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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(transactionManager = "authTransactionManager")
class OauthServiceRepositoryTest {
  @Autowired
  private lateinit var repository: OauthServiceRepository

  @Test
  fun givenATransientEntityItCanBePersisted() {
    val transientEntity = transientEntity()
    val entity = transientEntity()
    val persistedEntity = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.code).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findById(entity.code).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity.code).isEqualTo(transientEntity.code)
    assertThat(retrievedEntity.name).isEqualTo(transientEntity.name)
  }

  @Test
  fun findAllByEnabledTrueOrderByName() {
    assertThat(repository.findAllByEnabledTrueOrderByName()).extracting<String> { it.name }.contains("Digital Categorisation Service", "Home Detention Curfew", "Allocate a POM Service")
  }

  private fun transientEntity(): Service {
    return Service("CODE", "NAME", "Description", "SOME_ROLE", "http://some.url", true, "a@b.com")
  }
}

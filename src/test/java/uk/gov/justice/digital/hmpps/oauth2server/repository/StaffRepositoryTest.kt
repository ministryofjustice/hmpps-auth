package uk.gov.justice.digital.hmpps.oauth2server.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffRepository

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
class StaffRepositoryTest {
  @Autowired
  private lateinit var repository: StaffRepository

  @Test
  fun givenATransientEntityItCanBePeristed() {
    val transientEntity = transientEntity()
    val entity = transientEntity.toBuilder().build()
    val persistedEntity = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.staffId).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findById(persistedEntity.staffId).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity.status).isEqualTo(transientEntity.status)
  }

  private fun transientEntity() = Staff
      .builder()
      .firstName("TEST-FIRSTNAME")
      .lastName("TEST-LASTNAME")
      .status("ACTIVE")
      .staffId(-2L)
      .build()
}

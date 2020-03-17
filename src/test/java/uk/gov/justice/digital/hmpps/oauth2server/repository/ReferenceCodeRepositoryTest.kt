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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.DomainCodeIdentifier
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceCode
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceDomain.EMAIL_DOMAIN
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.ReferenceCodeRepository

@DataJpaTest
@ActiveProfiles("dev")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
class ReferenceCodeRepositoryTest {
  @Autowired
  private lateinit var repository: ReferenceCodeRepository

  @Test
  fun givenATransientEntityItCanBePersisted() {
    val transientEntity = transientEntity()
    val entity = transientEntity.toBuilder().build()
    val persistedEntity = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.domainCodeIdentifier).isNotNull
    TestTransaction.start()
    val retrievedEntity = repository.findById(entity.domainCodeIdentifier).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity.description).isEqualTo(transientEntity.description)
    assertThat(retrievedEntity.isActive).isEqualTo(transientEntity.isActive)
  }

  @Test
  fun givenAnExistingUserTheyCanBeRetrieved() {
    val retrievedEntity = repository.findById(DomainCodeIdentifier(EMAIL_DOMAIN, "PROBATION")).orElseThrow()
    assertThat(retrievedEntity.description).isEqualTo("HMIProbation.gov.uk")
    assertThat(retrievedEntity.isActive).isTrue()
  }

  @Test
  fun testFind() {
    val codes = repository.findByDomainCodeIdentifierDomainAndActiveIsTrueAndExpiredDateIsNull(EMAIL_DOMAIN)
    assertThat(codes).extracting("description").contains("%justice.gov.uk", "HMIProbation.gov.uk").hasSize(9)
  }

  private fun transientEntity() = ReferenceCode
      .builder()
      .domainCodeIdentifier(DomainCodeIdentifier(EMAIL_DOMAIN, "JOE"))
      .active(true)
      .description("some description")
      .build()
}

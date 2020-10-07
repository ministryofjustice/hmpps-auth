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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.UserCaseloadRole
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
class NomisUserPersonDetailsRepositoryTest {
  @Autowired
  private lateinit var repository: StaffUserAccountRepository

  @Test
  fun givenATransientEntityItCanBePeristed() {
    val transientEntity = transientEntity()
    val entity = transientEntity.toBuilder().build()
    val persistedEntity = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.username).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findById(entity.username).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity.username).isEqualTo(transientEntity.username)
    assertThat(retrievedEntity.type).isEqualTo(transientEntity.type)
  }

  @Test
  fun givenAnExistingUserTheyCanBeRetrieved() {
    val retrievedEntity = repository.findById("ITAG_USER").orElseThrow()
    assertThat(retrievedEntity.username).isEqualTo("ITAG_USER")
    assertThat(retrievedEntity.roles.stream().map { r: UserCaseloadRole -> r.role.name })
        .containsExactly("Some Old Role", "Omic Administrator", "KW Migration", "Maintain Access Roles Admin", "Global Search",
            "Create Category assessments", "Approve Category assessments", "Security Cat tool role")
    assertThat(retrievedEntity.filterRolesByCaseload("NWEB").stream().map { r: UserCaseloadRole -> r.role.name })
        .containsExactly("Omic Administrator", "KW Migration", "Maintain Access Roles Admin", "Global Search",
            "Create Category assessments", "Approve Category assessments", "Security Cat tool role")
  }

  @Test
  fun testSpare4MappedAsPasswordFromSysUserTable() {
    val entity = repository.findById("CA_USER").orElseThrow()
    assertThat(entity.password).startsWith("{bcrypt}")
  }

  @Test
  fun `find users by email address`() {
    val users = repository.findAllNomisUsersByEmailAddress("phillips@fredjustice.gov.uk")
    assertThat(users).extracting<String> { it.username }.containsExactly("RO_USER")
  }

  private fun transientEntity() = NomisUserPersonDetails
      .builder()
      .username("TEST_USER")
      .type("ADMIN")
      .staff(DEFAULT_STAFF)
      .build()

  companion object {
    private val DEFAULT_STAFF = Staff.builder().staffId(5L).build()
  }
}

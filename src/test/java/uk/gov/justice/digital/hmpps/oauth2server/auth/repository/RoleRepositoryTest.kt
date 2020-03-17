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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig

@DataJpaTest
@ActiveProfiles("dev")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(transactionManager = "authTransactionManager")
class RoleRepositoryTest {
  @Autowired
  private lateinit var repository: RoleRepository

  @Test
  fun givenATransientEntityItCanBePersisted() {
    val transientEntity = transientEntity()
    val entity = Authority(transientEntity.authority, transientEntity.roleName)
    val persistedEntity = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.authority).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findByRoleCode(entity.roleCode).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity.authority).isEqualTo(transientEntity.authority)
    assertThat(retrievedEntity.roleName).isEqualTo(transientEntity.roleName)
  }

  @Test
  fun givenAnExistingRoleTheyCanBeRetrieved() {
    val retrievedEntity = repository.findByRoleCode("PECS_POLICE").orElseThrow()
    assertThat(retrievedEntity.authority).isEqualTo("ROLE_PECS_POLICE")
    assertThat(retrievedEntity.roleName).isEqualTo("PECS Police")
  }

  @Test
  fun findAllByOrderByRoleName() {
    assertThat(repository.findAllByOrderByRoleName()).extracting<String> { obj: Authority -> obj.authority }.contains("ROLE_GLOBAL_SEARCH", "ROLE_PECS_POLICE")
  }

  @Test
  fun findByGroupAssignableRolesForUsername() {
    assertThat(repository.findByGroupAssignableRolesForUsername("AUTH_RO_VARY_USER")).extracting<String> { obj: Authority -> obj.roleCode }.containsExactly("GLOBAL_SEARCH", "LICENCE_RO", "LICENCE_VARY")
  }

  private fun transientEntity() = Authority("hdc", "Licences")
}

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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.GroupAssignableRole
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional("authTransactionManager")
class GroupRepositoryTest {
  @Autowired
  private lateinit var repository: GroupRepository

  @Autowired
  private lateinit var roleRepository: RoleRepository

  @Test
  fun givenATransientEntityItCanBePersisted() {
    val transientEntity = transientEntity()
    val entity = Group(transientEntity.groupCode, transientEntity.groupName)
    val persistedEntity = repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.groupCode).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findByGroupCode(entity.groupCode)

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(transientEntity)
    assertThat(retrievedEntity?.groupCode).isEqualTo(transientEntity.groupCode)
    assertThat(retrievedEntity?.groupName).isEqualTo(transientEntity.groupName)
  }

  @Test
  fun testRoleMapping() {
    val entity = repository.findByGroupCode("SITE_3_GROUP_1")
    assertThat(entity?.groupCode).isEqualTo("SITE_3_GROUP_1")
    assertThat(entity?.assignableRoles).isEmpty()
    val role1 = roleRepository.findByRoleCode("GLOBAL_SEARCH").orElseThrow()
    val role2 = roleRepository.findByRoleCode("LICENCE_RO").orElseThrow()
    val gar1 = GroupAssignableRole(role = role1, group = entity!!, automatic = false)
    entity.assignableRoles.add(gar1)
    val gar2 = GroupAssignableRole(role = role2, group = entity, automatic = true)
    entity.assignableRoles.add(gar2)
    repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity = repository.findByGroupCode("SITE_3_GROUP_1")
    val assignableRoles = retrievedEntity?.assignableRoles
    assertThat(assignableRoles).extracting<Authority> { obj: GroupAssignableRole -> obj.role }
      .extracting<String> { obj: Authority -> obj.roleCode }.containsOnly("GLOBAL_SEARCH", "LICENCE_RO")
    assignableRoles?.remove(gar1)
    assertThat(assignableRoles).containsExactly(gar2)
    repository.save(retrievedEntity!!)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity2 = repository.findByGroupCode("SITE_3_GROUP_1")
    assertThat(retrievedEntity2?.assignableRoles).containsOnly(gar2)
  }

  @Test
  fun givenAnExistingGroupTheyCanBeRetrieved() {
    val group = repository.findByGroupCode("SITE_1_GROUP_2")
    assertThat(group?.groupCode).isEqualTo("SITE_1_GROUP_2")
    assertThat(group?.groupName).isEqualTo("Site 1 - Group 2")
    assertThat(group?.assignableRoles).extracting<String> { it.role.roleCode }
      .containsOnly("GLOBAL_SEARCH", "LICENCE_RO")
    assertThat(group?.children).extracting<String> { it.groupCode }
      .containsOnly("CHILD_1")
  }

  @Test
  fun findAllByOrderByGroupName() {
    assertThat(repository.findAllByOrderByGroupName()).extracting<String> { it.groupCode }
      .containsSequence("SITE_1_GROUP_1", "SITE_1_GROUP_2", "SITE_2_GROUP_1", "SITE_3_GROUP_1")
  }

  private fun transientEntity(): Group = Group("hdc", "Licences")
}

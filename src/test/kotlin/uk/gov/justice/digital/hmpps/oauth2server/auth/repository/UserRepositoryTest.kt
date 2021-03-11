package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.assertj.core.api.AbstractListAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Contact
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType.MOBILE_PHONE
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType.SECONDARY_EMAIL
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType.EMAIL
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType.TEXT
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserFilter
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import java.time.LocalDateTime

@Suppress("UsePropertyAccessSyntax")
@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(transactionManager = "authTransactionManager")
class UserRepositoryTest {
  @Autowired
  private lateinit var repository: UserRepository

  @Autowired
  private lateinit var groupRepository: GroupRepository

  @Autowired
  private lateinit var roleRepository: RoleRepository

  @Autowired
  @Qualifier("authFlyway")
  private lateinit var flyway: Flyway

  @BeforeEach
  fun resetFlyway() {
    if (!initialized) {
      flyway.clean()
      flyway.migrate()
      initialized = true
    }
  }

  @Test
  fun givenATransientEntityItCanBePersisted() {
    val transientEntity =
      createSampleUser(
        username = "transiententity",
        email = "transient@b.com",
        mobile = "07700900321",
        source = delius,
        mfaPreference = TEXT
      )
    val persistedEntity = repository.save(transientEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.username).isNotNull()
    assertThat(persistedEntity.id).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findByUsername(transientEntity.username).orElseThrow()

    assertThat(retrievedEntity.username).isEqualTo(transientEntity.username)
    assertThat(retrievedEntity.email).isEqualTo(transientEntity.email)
    assertThat(retrievedEntity.mobile).isEqualTo(transientEntity.mobile)
    assertThat(retrievedEntity.mfaPreference).isEqualTo(TEXT)
  }

  @Test
  fun givenATransientAuthEntityItCanBePersisted() {
    val transientEntity =
      createSampleUser(username = "user", source = nomis, email = "a@b.com", firstName = "first", lastName = "last")
    val roleLicenceVary = roleRepository.findByRoleCode("LICENCE_VARY").orElseThrow()
    val roleGlobalSearch = roleRepository.findByRoleCode("GLOBAL_SEARCH").orElseThrow()
    transientEntity.authorities.addAll(setOf(roleLicenceVary, roleGlobalSearch))
    val persistedEntity = repository.save(transientEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.username).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findByUsername(transientEntity.username).orElseThrow()

    // equals only compares the business key columns
    assertThat(retrievedEntity).usingRecursiveComparison().ignoringFields("authorities").isEqualTo(transientEntity)
    assertThat(retrievedEntity.username).isEqualTo(transientEntity.username)
    assertThat(retrievedEntity.email).isEqualTo(transientEntity.email)
    assertThat(retrievedEntity.name).isEqualTo("first last")
    assertThat(retrievedEntity.authorities).extracting<String> { obj: Authority -> obj.authority }
      .containsOnly("ROLE_LICENCE_VARY", "ROLE_GLOBAL_SEARCH")
    assertThat(retrievedEntity.mfaPreference).isEqualTo(EMAIL)
  }

  @Test
  fun persistUserWithoutEmail() {
    val transientEntity = createSampleUser(username = "userb", source = nomis)
    val persistedEntity = repository.save(transientEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(persistedEntity.username).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findByUsername(transientEntity.username).orElseThrow()

    assertThat(retrievedEntity.username).isEqualTo(transientEntity.username)
    assertThat(retrievedEntity.email).isNull()
  }

  @Test
  fun givenAnExistingUserTheyCanBeRetrieved() {
    val retrievedEntity = repository.findByUsername("LOCKED_USER").orElseThrow()
    assertThat(retrievedEntity.username).isEqualTo("LOCKED_USER")
    assertThat(retrievedEntity.email).isEqualTo("locked@somewhere.com")
    assertThat(retrievedEntity.verified).isTrue()
    assertThat(retrievedEntity.mfaPreference).isEqualTo(EMAIL)
  }

  @Test
  fun givenAnExistingAuthUserTheyCanBeRetrieved() {
    val retrievedEntity = repository.findByUsername("AUTH_ADM").orElseThrow()
    assertThat(retrievedEntity.username).isEqualTo("AUTH_ADM")
    assertThat(retrievedEntity.person!!.firstName).isEqualTo("Auth")
    assertThat(retrievedEntity.authorities).extracting<String> { obj: Authority -> obj.authority }
      .containsOnly("ROLE_OAUTH_ADMIN", "ROLE_MAINTAIN_ACCESS_ROLES", "ROLE_MAINTAIN_OAUTH_USERS")
    assertThat(retrievedEntity.email).isEqualTo("auth_test2@digital.justice.gov.uk")
    assertThat(retrievedEntity.verified).isTrue()
    assertThat(retrievedEntity.mfaPreference).isEqualTo(EMAIL)
  }

  @Test
  fun testAuthorityMapping() {
    val entity = repository.findByUsername("AUTH_TEST").orElseThrow()
    assertThat(entity.username).isEqualTo("AUTH_TEST")
    assertThat(entity.name).isEqualTo("Auth Test")
    assertThat(entity.authorities).isEmpty()
    val roleLicenceVary = roleRepository.findByRoleCode("LICENCE_VARY").orElseThrow()
    val roleGlobalSearch = roleRepository.findByRoleCode("GLOBAL_SEARCH").orElseThrow()
    entity.authorities.add(roleLicenceVary)
    entity.authorities.add(roleGlobalSearch)
    repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity = repository.findByUsername("AUTH_TEST").orElseThrow()
    val authorities = retrievedEntity.authorities
    assertThat(authorities).extracting<String> { obj: Authority -> obj.authority }
      .containsOnly("ROLE_LICENCE_VARY", "ROLE_GLOBAL_SEARCH")
    authorities.removeIf { a: Authority -> "ROLE_LICENCE_VARY" == a.authority }
    assertThat(authorities).extracting<String> { obj: Authority -> obj.authority }.containsOnly("ROLE_GLOBAL_SEARCH")
    repository.save(retrievedEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity2 = repository.findByUsername("AUTH_TEST").orElseThrow()
    assertThat(retrievedEntity2.authorities).extracting<String> { obj: Authority -> obj.authority }
      .containsOnly("ROLE_GLOBAL_SEARCH")
  }

  @Test
  fun testGroupMapping() {
    val entity = repository.findByUsername("AUTH_TEST").orElseThrow()
    assertThat(entity.username).isEqualTo("AUTH_TEST")
    assertThat(entity.name).isEqualTo("Auth Test")
    assertThat(TestTransaction.isActive()).isTrue()
    assertThat(entity.groups).isEmpty()
    val group1 = groupRepository.findByGroupCode("SITE_1_GROUP_1")
    val group3 = groupRepository.findByGroupCode("SITE_3_GROUP_1")
    entity.groups.add(group1!!)
    entity.groups.add(group3!!)
    repository.save(entity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity = repository.findByUsername("AUTH_TEST").orElseThrow()
    val groups = retrievedEntity.groups
    assertThat(groups).extracting<String> { obj: Group -> obj.groupCode }
      .containsOnly("SITE_1_GROUP_1", "SITE_3_GROUP_1")
    groups.removeIf { a: Group -> "SITE_3_GROUP_1" == a.groupCode }
    assertThat(groups).extracting<String> { obj: Group -> obj.groupCode }.containsOnly("SITE_1_GROUP_1")
    repository.save(retrievedEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity2 = repository.findByUsername("AUTH_TEST").orElseThrow()
    assertThat(retrievedEntity2.groups).extracting<String> { obj: Group -> obj.groupCode }
      .containsOnly("SITE_1_GROUP_1")
  }

  @Test
  fun `test persist contact mapping`() {
    val transientEntity = createSampleUser(username = "contact", source = nomis)
    transientEntity.addContact(SECONDARY_EMAIL, "some value")
    transientEntity.addContact(SECONDARY_EMAIL, "some replacement value")
    repository.save(transientEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity = repository.findByUsername(transientEntity.username).orElseThrow()

    assertThat(retrievedEntity.contacts).containsExactly(Contact(SECONDARY_EMAIL, "some replacement value"))
  }

  @Test
  fun `test persist contact mapping 2`() {
    val transientEntity = createSampleUser(username = "contact2", source = nomis)
    transientEntity.addContact(SECONDARY_EMAIL, "some value")
    transientEntity.addContact(MOBILE_PHONE, "some mobile")
    repository.save(transientEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity = repository.findByUsername(transientEntity.username).orElseThrow()
    retrievedEntity.findContact(SECONDARY_EMAIL).ifPresent { it.verified = true }
    assertThat(retrievedEntity.contacts).containsExactlyInAnyOrder(
      Contact(SECONDARY_EMAIL, "some value", true),
      Contact(MOBILE_PHONE, "some mobile", false)
    )
    repository.save(retrievedEntity)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val retrievedEntity2 = repository.findByUsername(transientEntity.username).orElseThrow()
    assertThat(retrievedEntity2.contacts).containsExactlyInAnyOrder(
      Contact(SECONDARY_EMAIL, "some value", true),
      Contact(MOBILE_PHONE, "some mobile", false)
    )
  }

  @Test
  fun `test retrieve contact mapping`() {
    val retrievedEntity = repository.findByUsername("AUTH_ADM").orElseThrow()

    assertThat(retrievedEntity.contacts).containsExactly(Contact(SECONDARY_EMAIL, "john@smith.com"))
  }

  @Test
  fun findByUsernameAndMasterIsTrue_AuthUser() {
    assertThat(repository.findByUsernameAndMasterIsTrue("AUTH_TEST")).isPresent
  }

  @Test
  fun findByUsernameAndMasterIsTrue_NomisUser() {
    assertThat(repository.findByUsernameAndMasterIsTrue("ITAG_USER")).isEmpty
  }

  @Test
  fun findByEmail() {
    assertThat(repository.findByEmail("auth_test2@digital.justice.gov.uk")).extracting<String> { it.username }
      .contains("AUTH_ADM", "AUTH_EXPIRED")
  }

  @Test
  fun findByEmail_NoRecords() {
    assertThat(repository.findByEmail("noone@digital.justice.gov.uk")).isEmpty()
  }

  @Test
  fun findByEmailAndMasterIsTrue() {
    assertThat(repository.findByEmailAndMasterIsTrueOrderByUsername("auth_test2@digital.justice.gov.uk"))
      .extracting<String> { it.username }
      .contains("AUTH_ADM", "AUTH_EXPIRED")
  }

  @Test
  fun findByEmailAndMasterIsTrue_NomisUser() {
    assertThat(repository.findByEmailAndMasterIsTrueOrderByUsername("ca_user@digital.justice.gov.uk")).isEmpty()
  }

  @Test
  fun findAll_UserFilter_ByRole() {
    assertThat(repository.findAll(UserFilter(roleCodes = listOf("LICENCE_VARY"))))
      .extracting<String> { it.username }
      .containsExactly("AUTH_RO_VARY_USER")
  }

  @Test
  fun findAll_UserFilter_ByRoles() {
    assertThat(repository.findAll(UserFilter(roleCodes = listOf("  LICENCE_VARY", "  LICENCE_RO"))))
      .extracting<String> { it.username }
      .containsExactly("AUTH_DELETEALL", "AUTH_RO_USER_TEST", "AUTH_RO_USER", "AUTH_RO_VARY_USER")
  }

  @Test
  fun findAll_UserFilter_ByGroup() {
    assertThat(repository.findAll(UserFilter(groupCodes = listOf("SITE_1_GROUP_2"))))
      .extracting<String> { it.username }
      .containsExactly("AUTH_GROUP_MANAGER", "AUTH_RO_VARY_USER")
  }

  @Test
  fun findAll_UserFilter_ByGroups() {
    assertThat(repository.findAll(UserFilter(groupCodes = listOf("SITE_1_GROUP_2  ", "  SITE_1_GROUP_3"))))
      .extracting<String> { it.username }
      .containsExactly("AUTH_GROUP_MANAGER", "AUTH_RO_VARY_USER")
  }

  @Test
  fun findAll_UserFilter_ByGroups_IgnoreMultipleSources() {
    assertThat(repository.findAll(UserFilter(groupCodes = listOf("SITE_1_GROUP_2  ", "  SITE_1_GROUP_3"), userSources = listOf(auth, nomis, delius))))
      .extracting<String> { it.username }
      .containsExactly("AUTH_GROUP_MANAGER", "AUTH_RO_VARY_USER")
  }

  @Test
  fun findAll_UserFilter_ByUsername() {
    assertThat(repository.findAll(UserFilter(name = "_expired")))
      .extracting<String> { it.username }
      .contains("AUTH_EXPIRED", "AUTH_MFA_EXPIRED_USER")
  }

  @Test
  fun findAll_UserFilter_ByEmail() {
    assertThat(repository.findAll(UserFilter(name = "test@digital")))
      .extracting<String> { it.username }
      .containsOnly("AUTH_TEST", "AUTH_RO_USER_TEST", "AUTH_CHANGE_TEST", "AUTH_CHANGE2_TEST", "AUTH_CHANGE_EMAIL")
  }

  @Test
  fun findAll_UserFilter_ByFirstNameLastName() {
    assertThat(repository.findAll(UserFilter(name = "a no")))
      .extracting<String> { it.username }
      .containsExactly(
        "AUTH_NO_EMAIL",
        "AUTH_MFA_NOEMAIL_USER",
        "AUTH_MFA_NOTEXT_USER",
        "AUTH_MFA_PREF_TEXT_EMAIL",
        "NOMIS_LOCKED_AUTH_DISABLED",
        "DELIUS_ENABLED_AUTH_DISABLED",
        "NOMIS_ENABLED_AUTH_DISABLED",
      )
  }

  @Test
  fun findAll_UserFilter_ByLastNameFirstName() {
    assertThat(repository.findAll(UserFilter(name = "orton, r")))
      .extracting<String> { it.username }
      .containsOnly(
        "AUTH_RO_USER",
        "AUTH_RO_VARY_USER",
        "AUTH_RO_USER1@DIGITAL.JUSTICE.GOV.UK",
        "AUTH_RO_USER_TEST",
        "AUTH_RO_USER_TEST2",
        "AUTH_RO_USER_TEST3",
      )
  }

  @Test
  fun findAll_UserFilter_ByAuthSourcesMultiple() {
    assertThat(repository.findAll(UserFilter(name = "orton, r", userSources = listOf(auth, nomis))))
      .extracting<String> { it.username }
      .containsOnly(
        "AUTH_RO_USER",
        "AUTH_RO_VARY_USER",
        "AUTH_RO_USER1@DIGITAL.JUSTICE.GOV.UK",
        "AUTH_RO_USER_TEST",
        "AUTH_RO_USER_TEST2",
        "AUTH_RO_USER_TEST3",
      )
  }

  @Test
  fun findAll_UserFilter_all() {
    assertThat(
      repository.findAll(
        UserFilter(roleCodes = listOf("LICENCE_VARY"), groupCodes = listOf("SITE_1_GROUP_2"), name = "vary")
      )
    )
      .extracting<String> { it.username }
      .containsExactly("AUTH_RO_VARY_USER")
  }

  @Test
  fun `findAll UserFilter ActiveOnly`() {
    assertThat(repository.findAll(UserFilter(name = "a no", status = UserFilter.Status.ACTIVE)))
      .extracting<String> { it.username }
      .containsExactly(
        "AUTH_NO_EMAIL",
        "AUTH_MFA_NOEMAIL_USER",
        "AUTH_MFA_NOTEXT_USER",
        "AUTH_MFA_PREF_TEXT_EMAIL",
      )
  }

  @Test
  fun `findAll UserFilter InactiveOnly`() {
    assertThat(repository.findAll(UserFilter(name = "a no", status = UserFilter.Status.INACTIVE)))
      .extracting<String> { it.username }
      .containsExactly(
        "NOMIS_LOCKED_AUTH_DISABLED",
        "DELIUS_ENABLED_AUTH_DISABLED",
        "NOMIS_ENABLED_AUTH_DISABLED",
      )
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun findInactiveUsers_First10() {
    val inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(
      LocalDateTime.now().plusMinutes(1)
    )
    val abstractListAssert = assertThat(inactive).extracting<String> { it.username }
      .contains("AUTH_INACTIVE") as AbstractListAssert<*, MutableList<out String>, String, ObjectAssert<String>>
    abstractListAssert.doesNotContain("AUTH_DISABLED", "ITAG_USER")
    assertThat(inactive).hasSize(10)
  }

  @Test
  fun findInactiveUsers_OrderByLastLoggedInOldestFirst() {
    val inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(
      LocalDateTime.now().plusMinutes(1)
    )
    assertThat(inactive).extracting<String> { it.username }.first().isEqualTo("AUTH_USER_LAST_LOGIN")
  }

  @Test
  fun findInactiveUsers_NoRows() {
    val inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(
      LocalDateTime.parse("2019-01-01T12:00:00").minusSeconds(1)
    )
    assertThat(inactive).isEmpty()
  }

  @Test
  fun findInactiveUsers_SingleRow() {
    val inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(
      LocalDateTime.parse("2019-02-03T13:23:19").plusSeconds(1)
    )
    assertThat(inactive).extracting<String> { it.username }.containsExactly("AUTH_USER_LAST_LOGIN", "AUTH_INACTIVE")
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun findDisabledUsers_First10() {
    val inactive =
      repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1))
    val abstractListAssert = assertThat(inactive).extracting<String>(User::getUsername).contains(
      "AUTH_DELETE",
      "AUTH_DELETEALL",
      "NOMIS_DELETE"
    ) as AbstractListAssert<*, MutableList<out String>, String, ObjectAssert<String>>
    abstractListAssert.doesNotContain("AUTH_DISABLED", "AUTH_USER")
    assertThat(inactive).hasSize(10)
  }

  @Test
  fun findDisabledUsers_OrderByLastLoggedInOldestFirst() {
    val inactive =
      repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(LocalDateTime.now().plusMinutes(1))
    assertThat(inactive).extracting<String> { it.username }.first().isEqualTo("AUTH_DELETE")
  }

  @Test
  fun findDisabledUsers_NoRows() {
    val inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(
      LocalDateTime.parse("2018-01-02T13:23:19").minusSeconds(1)
    )
    assertThat(inactive).isEmpty()
  }

  @Test
  fun findDisabledUsers_SingleRow() {
    val inactive = repository.findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(
      LocalDateTime.parse("2018-01-02T13:23:19").plusSeconds(1)
    )
    assertThat(inactive).extracting<String> { it.username }.containsExactly("AUTH_DELETE")
  }

  companion object {
    private var initialized = false
  }
}

package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.GroupAssignableRole
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientApi
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import javax.persistence.EntityNotFoundException

@Suppress("UsePropertyAccessSyntax")
class AuthUserServiceTest {
  private val userRepository: UserRepository = mock()
  private val notificationClient: NotificationClientApi = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val authUserGroupService: AuthUserGroupService = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val passwordEncoder: PasswordEncoder = mock()
  private val oauthServiceRepository: OauthServiceRepository = mock()
  private var authUserService = AuthUserService(userRepository, notificationClient, telemetryClient, verifyEmailService, authUserGroupService, maintainUserCheck, passwordEncoder, oauthServiceRepository, "licences", 90, 10)

  @BeforeEach
  fun setUp() {
    mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link")
  }

  @Test
  fun createUser_usernameLength() {
    assertThatThrownBy { authUserService.createUser("user", "email", "first", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER) }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field username with reason: length")
  }

  @Test
  fun createUser_usernameMaxLength() {
    assertThatThrownBy { authUserService.createUser("ThisIsLongerThanTheAllowedUsernameLength", "email", "first", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER) }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field username with reason: maxlength")
  }

  @Test
  fun createUser_usernameFormat() {
    assertThatThrownBy { authUserService.createUser("user-name", "email", "first", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER) }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field username with reason: format")
  }

  @Test
  fun createUser_firstNameLength() {
    assertThatThrownBy { authUserService.createUser("userme", "email", "s", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER) }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field firstName with reason: length")
  }

  @Test
  fun createUser_firstNameMaxLength() {
    assertThatThrownBy { authUserService.createUser("userme", "email", "ThisFirstNameIsMoreThanFiftyCharactersInLengthAndInvalid", "last", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER) }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field firstName with reason: maxlength")
  }

  @Test
  fun createUser_lastNameLength() {
    assertThatThrownBy { authUserService.createUser("userme", "email", "se", "x", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER) }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field lastName with reason: length")
  }

  @Test
  fun createUser_lastNameMaxLength() {
    assertThatThrownBy { authUserService.createUser("userme", "email", "se", "ThisLastNameIsMoreThanFiftyCharactersInLengthAndInvalid", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER) }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field lastName with reason: maxlength")
  }

  @Test
  fun createUser_emailValidation() {
    doThrow(VerifyEmailException("reason")).whenever(verifyEmailService).validateEmailAddress(anyString(), eq(EmailType.PRIMARY))
    assertThatThrownBy { authUserService.createUser("userme", "email", "se", "xx", null, "url", "bob", GRANTED_AUTHORITY_SUPER_USER) }.isInstanceOf(VerifyEmailException::class.java).hasMessage("Verify email failed with reason: reason")
    verify(verifyEmailService).validateEmailAddress("email", EmailType.PRIMARY)
  }

  @Test
  fun createUser_successLinkReturned() {
    val link = authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    assertThat(link).startsWith("url?token=").hasSize("url?token=".length + 36)
  }

  @Test
  fun createUser_trackSuccess() {
    authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(telemetryClient).trackEvent("AuthUserCreateSuccess", mapOf("username" to "USERME", "admin" to "bob"), null)
  }

  @Test
  fun createUser_saveUserRepository() {
    val link = authUserService.createUser("userme", "email", "se", "xx", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(userRepository).save<User>(check {
      val userToken = it.tokens.stream().findFirst().orElseThrow()
      assertThat(userToken.tokenType).isEqualTo(RESET)
      assertThat(userToken.token).isEqualTo(link.substring("url?token=".length))
      assertThat(userToken.tokenExpiry).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8))
    })
  }

  @Test
  fun createUser_saveEmailRepository() {
    authUserService.createUser("userMe", "eMail", "first", "last", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(userRepository).save<User>(check {
      with(it) {
        assertThat(name).isEqualTo("first last")
        assertThat(email).isEqualTo("email")
        assertThat(username).isEqualTo("USERME")
        assertThat(password).isNull()
        assertThat(isMaster).isTrue()
        assertThat(isVerified).isFalse()
        assertThat(isCredentialsNonExpired).isFalse()
        assertThat(authorities).isEmpty()
      }
    })
  }

  @Test
  fun createUser_trimName() {
    authUserService.createUser("userMe", "eMail", "first  ", "  last ", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(userRepository).save<User>(check {
      assertThat(it.name).isEqualTo("first last")
    })
  }

  @Test
  fun createUser_formatEmailInput() {
    authUserService.createUser("userMe", "    SARAH.o’connor@gov.uk", "first", "last", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(userRepository).save<User>(check {
      assertThat(it.email).isEqualTo("sarah.o'connor@gov.uk")
    })
  }

  @Test
  fun createUser_setGroup() {
    whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(listOf(Group("SITE_1_GROUP_1", "desc")))
    authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(userRepository).save<User>(check {
      assertThat(it.groups).extracting<String> { it.groupCode }.containsOnly("SITE_1_GROUP_1")
    })
  }

  @Test
  fun createUser_noRoles() {
    whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(listOf(Group("SITE_1_GROUP_1", "desc")))
    authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(userRepository).save<User>(check {
      assertThat(it.authorities).isEmpty()
    })
  }

  @Test
  fun createUser_setRoles() {
    val group = Group("SITE_1_GROUP_1", "desc")
    group.assignableRoles = setOf(
        GroupAssignableRole(Authority("AUTH_AUTO", "Auth Name"), group, true),
        GroupAssignableRole(Authority("AUTH_MANUAL", "Auth Name"), group, false))
    whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(listOf(group))
    authUserService.createUser("userMe", "eMail", "first", "last", "SITE_1_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(userRepository).save<User>(check {
      assertThat(it.authorities).extracting<String> { it.roleCode }.containsOnly("AUTH_AUTO")
    })
  }

  @Test
  fun createUser_wrongGroup() {
    whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(listOf(Group("OTHER_GROUP", "desc")))
    assertThatThrownBy { authUserService.createUser("userMe", "eMail", "first", "last", "SITE_2_GROUP_1", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER) }
        .isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field groupCode with reason: notfound")
  }

  @Test
  fun createUser_missingGroup() {
    assertThatThrownBy { authUserService.createUser("userMe", "eMail", "first", "last", "", "url?token=", "bob", setOf()) }
        .isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field groupCode with reason: missing")
  }

  @Test
  fun createUser_callNotify() {
    val link = authUserService.createUser("userme", "email", "first", "last", null, "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(notificationClient).sendEmail("licences", "email", mapOf("resetLink" to link, "firstName" to "first last", "fullName" to "first last", "supportLink" to "nomis_support_link"), null)
  }

  @Test
  fun createUser_pecsUserGroupSupportLink() {
    mockServiceOfNameWithSupportLink("BOOK_MOVE", "book_move_support_link")
    whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(listOf(Group("PECS_GROUP", "desc")))
    authUserService.createUser("userMe", "eMail", "first", "last", "PECS_GROUP", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(notificationClient).sendEmail(anyString(), anyString(), check {
      assertThat(it["supportLink"]).isEqualTo("book_move_support_link")
    }, isNull())
  }

  @Test
  fun createUser_nonPecsUserGroupSupportLink() {
    authUserService.createUser("userMe", "eMail", "first", "last", "", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(notificationClient).sendEmail(anyString(), anyString(), check {
      assertThat(it["supportLink"]).isEqualTo("nomis_support_link")
    }, isNull())
  }

  @Test
  fun createUser_noGroupsSupportLink() {
    authUserService.createUser("userMe", "eMail", "first", "last", "", "url?token=", "bob", GRANTED_AUTHORITY_SUPER_USER)
    verify(notificationClient).sendEmail(anyString(), anyString(), check {
      assertThat(it["supportLink"]).isEqualTo("nomis_support_link")
    }, isNull())
  }

  @Test
  fun amendUserEmail_emailValidation() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser())
    doThrow(VerifyEmailException("reason")).whenever(verifyEmailService).validateEmailAddress(anyString(), eq(EmailType.PRIMARY))
    assertThatThrownBy { authUserService.amendUserEmail("userme", "email", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY) }.isInstanceOf(VerifyEmailException::class.java).hasMessage("Verify email failed with reason: reason")
    verify(verifyEmailService).validateEmailAddress("email", EmailType.PRIMARY)
  }

  @Test
  fun amendUserEmail_groupValidation() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser())
    doThrow(AuthUserGroupRelationshipException("user", "reason")).whenever(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString(), any(), any())
    assertThatThrownBy { authUserService.amendUserEmail("userme", "email", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY) }.isInstanceOf(AuthUserGroupRelationshipException::class.java).hasMessage("Unable to maintain user: user with reason: reason")
  }

  @Test
  fun amendUserEmail_successLinkReturned() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser())
    val link = authUserService.amendUserEmail("userme", "email", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
    assertThat(link).startsWith("url?token=").hasSize("url?token=".length + 36)
  }

  @Test
  fun amendUserEmail_trackSuccess() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser())
    authUserService.amendUserEmail("userme", "email", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
    verify(telemetryClient).trackEvent("AuthUserAmendSuccess", mapOf("username" to "someuser", "admin" to "bob"), null)
  }

  @Test
  fun amendUserEmail_saveTokenRepository() {
    val user = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(user)
    val link = authUserService.amendUserEmail("userme", "email", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
    val userToken = user.orElseThrow().tokens.stream().findFirst().orElseThrow()
    assertThat(userToken.tokenType).isEqualTo(RESET)
    assertThat(userToken.token).isEqualTo(link.substring("url?token=".length))
    assertThat(userToken.tokenExpiry).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8))
  }

  @Test
  fun amendUserEmail_saveEmailRepository() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser())
    authUserService.amendUserEmail("userMe", "eMail", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
    verify(userRepository).save<User>(check {
      assertThat(it.email).isEqualTo("email")
    })
  }

  @Test
  fun amendUserEmail_formatEmailInput() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUser())
    authUserService.amendUserEmail("userMe", "    SARAH.o’connor@gov.uk", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
    verify(userRepository).save<User>(check {
      assertThat(it.email).isEqualTo("sarah.o'connor@gov.uk")
    })
  }

  @Test
  fun amendUserEmail_pecsUserGroupSupportLink() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups("PECS_GROUP")))
    mockServiceOfNameWithSupportLink("BOOK_MOVE", "book_move_support_link")
    authUserService.amendUserEmail("ANY_USER_NAME", "ANY_USER-EMAIL", "ANY_URL", "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER, EmailType.PRIMARY)
    verify(notificationClient).sendEmail(anyString(), anyString(), check {
      assertThat(it["supportLink"]).isEqualTo("book_move_support_link")
    }, isNull())
  }

  @Test
  fun amendUserEmail_nonPecsUserGroupSupportLink() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups("NON_PECS_GROUP")))
    authUserService.amendUserEmail("ANY_USER_NAME", "ANY_USER-EMAIL", "ANY_URL", "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER, EmailType.PRIMARY)
    verify(notificationClient).sendEmail(anyString(), anyString(), check {
      assertThat(it["supportLink"]).isEqualTo("nomis_support_link")
    }, isNull())
  }

  @Test
  fun amendUserEmail_onePecsGroupOfManySupportLink() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups("NON_PECS_GROUP", "PECS_GROUP")))
    mockServiceOfNameWithSupportLink("BOOK_MOVE", "book_move_support_link")
    authUserService.amendUserEmail("ANY_USER_NAME", "ANY_USER-EMAIL", "ANY_URL", "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER, EmailType.PRIMARY)
    verify(notificationClient).sendEmail(anyString(), anyString(), check {
      assertThat(it["supportLink"]).isEqualTo("book_move_support_link")
    }, isNull())
  }

  @Test
  fun amendUserEmail_noGroupSupportLink() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups()))
    authUserService.amendUserEmail("ANY_USER_NAME", "ANY_USER-EMAIL", "ANY_URL", "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER, EmailType.PRIMARY)
    verify(notificationClient).sendEmail(anyString(), anyString(), check {
      assertThat(it["supportLink"]).isEqualTo("nomis_support_link")
    }, isNull())
  }

  @Test
  fun amendUserEmail_unverifiedEmail_sendsInitialEmail() {
    val userUnverifiedEmail = User.builder().username("SOME_USER_NAME").verified(false).build()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userUnverifiedEmail))
    authUserService.amendUserEmail(
        "SOME_USER_NAME",
        "some_user_email@gov.uk",
        "ANY_HOST/initial-password?token=SOME_TOKEN",
        "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY)
    verify(notificationClient).sendEmail(anyString(), anyString(), any(), isNull())
    verify(verifyEmailService, never()).requestVerification(anyString(), anyString(), anyString(), anyString(), eq(EmailType.PRIMARY))
  }

  @Test
  fun amendUserEmail_verifiedEmail_requestsVerification() {
    val userVerifiedEmail = User.builder().username("SOME_USER_NAME").verified(true).build()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userVerifiedEmail))
    whenever(verifyEmailService.requestVerification(anyString(), anyString(), anyString(), anyString(), eq(EmailType.PRIMARY))).thenReturn("SOME_VERIFY_LINK")
    authUserService.amendUserEmail(
        "SOME_USER_NAME",
        "some_user_email@gov.uk",
        "SOME_HOST/initial-password?token=SOME_TOKEN",
        "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY)
    verify(verifyEmailService).requestVerification("SOME_USER_NAME", "some_user_email@gov.uk", "SOME_USER_NAME", "SOME_HOST/verify-email-confirm?token=SOME_TOKEN", EmailType.PRIMARY)
    verify(notificationClient, never()).sendEmail(anyString(), anyString(), any(), anyString())
  }


  @Test
  fun amendUserEmail_verifiedEmail_savesUnverifiedUser() {
    whenever(verifyEmailService.requestVerification(anyString(), anyString(), anyString(), anyString(), eq(EmailType.PRIMARY))).thenReturn("SOME_VERIFY_LINK")
    val userVerifiedEmail = User.builder().username("SOME_USER_NAME").verified(true).build()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userVerifiedEmail))
    authUserService.amendUserEmail(
        "SOME_USER_NAME",
        "some_user_email@gov.uk",
        "ANY_HOST/initial-password?token=SOME_TOKEN",
        "ANY_ADMIN", GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY)
    assertThat(userVerifiedEmail.isVerified).isFalse()
  }

  private fun userOfGroups(vararg groupList: String): User {
    val userPerson = Person("ANY_FIRST_NAME", "ANY_LAST_NAME")
    val groups = Arrays.stream(groupList).map { Group(it, "any desc") }.collect(Collectors.toSet())
    return User.builder().groups(groups).email("ANY_EMAIL").person(userPerson).username("ANY ANY").build()
  }

  private fun mockServiceOfNameWithSupportLink(serviceCode: String, supportLink: String) {
    val service = Service(serviceCode, "service", "service", "ANY_ROLES", "ANY_URL", true, supportLink)
    whenever(oauthServiceRepository.findById(serviceCode)).thenReturn(Optional.of(service))
  }

  @Test
  fun authUserByUsername() {
    val createdUser = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createdUser)
    val user = authUserService.getAuthUserByUsername("   bob   ")
    assertThat(user).isPresent.get().isEqualTo(createdUser.orElseThrow())
    verify(userRepository).findByUsernameAndMasterIsTrue("BOB")
  }

  @Test
  fun findByEmailAndMasterIsTrue() {
    whenever(userRepository.findByEmailAndMasterIsTrueOrderByUsername(anyString())).thenReturn(listOf(User.of("someuser")))
    val user = authUserService.findAuthUsersByEmail("  bob  ")
    assertThat(user).extracting<String> { it.username }.containsOnly("someuser")
  }

  @Test
  fun findAuthUsersByEmail_formatEmailAddress() {
    whenever(userRepository.findByEmailAndMasterIsTrueOrderByUsername(anyString())).thenReturn(listOf(User.of("someuser")))
    authUserService.findAuthUsersByEmail("  some.u’ser@SOMEwhere  ")
    verify(userRepository).findByEmailAndMasterIsTrueOrderByUsername("some.u'ser@somewhere")
  }

  @Test
  fun enableUser_superUser() {
    val optionalUser = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.enableUser("user", "admin", SUPER_USER)
    assertThat(optionalUser).get().extracting { it.isEnabled }.isEqualTo(true)
    verify(userRepository).save(optionalUser.orElseThrow())
  }

  @Test
  fun enableUser_invalidGroup_GroupManager() {
    val optionalUser = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    doThrow(AuthUserGroupRelationshipException("someuser", "User not with your groups")).whenever(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString(), anyCollection(), any())
    assertThatThrownBy { authUserService.enableUser("someuser", "admin", GROUP_MANAGER) }.isInstanceOf(AuthUserGroupRelationshipException::class.java).hasMessage("Unable to maintain user: someuser with reason: User not with your groups")
  }

  @Test
  fun enableUser_validGroup_groupManager() {
    val user = User.of("user")
    val group1 = Group("group", "desc")
    user.groups = setOf(group1, Group("group2", "desc"))
    user.authorities = HashSet(listOf(Authority("JOE", "bloggs")))
    val groupManager = User.of("groupManager")
    groupManager.groups = setOf(Group("group3", "desc"), group1)
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
    authUserService.enableUser("user", "admin", GROUP_MANAGER)
    assertThat(user).extracting { it.isEnabled }.isEqualTo(true)
    verify(userRepository).save(user)
  }

  @Test
  fun enableUser_NotFound() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy { authUserService.enableUser("user", "admin", SUPER_USER) }.isInstanceOf(EntityNotFoundException::class.java).hasMessageContaining("username user")
  }

  @Test
  fun enableUser_trackEvent() {
    val optionalUser = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.enableUser("someuser", "someadmin", SUPER_USER)
    verify(telemetryClient).trackEvent("AuthUserChangeEnabled", mapOf("username" to "someuser", "admin" to "someadmin", "enabled" to "true"), null)
  }

  @Test
  fun enableUser_setLastLoggedIn() {
    val optionalUser = createUser()
    val user = optionalUser.orElseThrow()
    val tooLongAgo = LocalDateTime.now().minusDays(95)
    user.lastLoggedIn = tooLongAgo
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.enableUser("someuser", "someadmin", SUPER_USER)
    assertThat(user.lastLoggedIn).isBetween(LocalDateTime.now().minusDays(84), LocalDateTime.now().minusDays(82))
  }

  @Test
  fun enableUser_leaveLastLoggedInAlone() {
    val optionalUser = createUser()
    val user = optionalUser.orElseThrow()
    val fiveDaysAgo = LocalDateTime.now().minusDays(5)
    user.lastLoggedIn = fiveDaysAgo
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.enableUser("someuser", "someadmin", SUPER_USER)
    assertThat(user.lastLoggedIn).isEqualTo(fiveDaysAgo)
  }

  @Test
  fun disableUser_superUser() {
    val optionalUser = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.disableUser("user", "admin", SUPER_USER)
    assertThat(optionalUser).get().extracting { it.isEnabled }.isEqualTo(false)
    verify(userRepository).save(optionalUser.orElseThrow())
  }

  @Test
  fun disableUser_invalidGroup_GroupManager() {
    val optionalUser = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    doThrow(AuthUserGroupRelationshipException("someuser", "User not with your groups")).whenever(maintainUserCheck).ensureUserLoggedInUserRelationship(anyString(), anyCollection(), any())
    assertThatThrownBy { authUserService.disableUser("someuser", "admin", GROUP_MANAGER) }.isInstanceOf(AuthUserGroupRelationshipException::class.java).hasMessage("Unable to maintain user: someuser with reason: User not with your groups")
  }

  @Test
  fun disableUser_validGroup_groupManager() {
    val user = User.of("user")
    val group1 = Group("group", "desc")
    user.groups = setOf(group1, Group("group2", "desc"))
    user.isEnabled = true
    user.authorities = HashSet(listOf(Authority("JOE", "bloggs")))
    val groupManager = User.of("groupManager")
    groupManager.groups = setOf(Group("group3", "desc"), group1)
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(user))
    authUserService.disableUser("user", "admin", GROUP_MANAGER)
    assertThat(user).extracting { it.isEnabled }.isEqualTo(false)
    verify(userRepository).save(user)
  }

  @Test
  fun disableUser_trackEvent() {
    val optionalUser = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.disableUser("someuser", "someadmin", SUPER_USER)
    verify(telemetryClient).trackEvent("AuthUserChangeEnabled", mapOf("username" to "someuser", "admin" to "someadmin", "enabled" to "false"), null)
  }

  @Test
  fun disableUser_notFound() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy { authUserService.disableUser("user", "admin", SUPER_USER) }.isInstanceOf(EntityNotFoundException::class.java).hasMessageContaining("username user")
  }

  private fun createUser() = Optional.of(User.of("someuser"))

  @Test
  fun findAuthUsers() {
    whenever(userRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
    val unpaged = Pageable.unpaged()
    authUserService.findAuthUsers("somename ", "somerole  ", "  somegroup", unpaged)
    verify(userRepository).findAll(check {
      assertThat(it).extracting("name", "roleCode", "groupCode").containsExactly("somename", "somerole", "somegroup")
    }, eq(unpaged))
  }

  @Test
  fun lockUser_alreadyExists() {
    val user = User.builder().username("user").build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    authUserService.lockUser(user)
    assertThat(user.isLocked).isTrue()
    verify(userRepository).save(user)
  }

  @Test
  fun lockUser_newUser() {
    val user = staffUserAccountForBob
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    authUserService.lockUser(user)
    verify(userRepository).save<User>(check {
      assertThat(it.isLocked).isTrue()
      assertThat(it.username).isEqualTo("bob")
      assertThat(it.source).isEqualTo(nomis)
    })
  }

  @Test
  fun unlockUser_alreadyExists() {
    val user = User.builder().username("user").locked(true).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    authUserService.unlockUser(user)
    assertThat(user.isLocked).isFalse()
    assertThat(user.isVerified).isTrue()
    verify(userRepository).save(user)
  }

  @Test
  fun unlockUser_newUser() {
    val user = staffUserAccountForBob
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    authUserService.unlockUser(user)
    verify(userRepository).save<User>(check {
      assertThat(it.isLocked).isFalse()
      assertThat(it.isVerified).isTrue()
      assertThat(it.username).isEqualTo("bob")
      assertThat(it.source).isEqualTo(nomis)
    })
  }

  @Test
  fun changePassword() {
    val user = User.builder().username("user").build()
    whenever(passwordEncoder.encode(anyString())).thenReturn("hashedpassword")
    authUserService.changePassword(user, "pass")
    assertThat(user.password).isEqualTo("hashedpassword")
    assertThat(user.passwordExpiry).isAfterOrEqualTo(LocalDateTime.now().plusDays(9))
    verify(passwordEncoder).encode("pass")
  }

  @Test
  fun changePassword_PasswordSameAsCurrent() {
    val user = User.builder().username("user").build()
    user.password = "oldencryptedpassword"
    whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)
    assertThatThrownBy { authUserService.changePassword(user, "pass") }.isInstanceOf(ReusedPasswordException::class.java)
    verify(passwordEncoder).matches("pass", "oldencryptedpassword")
  }

  @Test
  fun amendUser_firstNameLength() {
    assertThatThrownBy { authUserService.amendUser("userme", "s", "last") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field firstName with reason: length")
  }

  @Test
  fun amendUser_firstNameBlank() {
    assertThatThrownBy { authUserService.amendUser("userme", "  ", "last") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field firstName with reason: required")
  }

  @Test
  fun amendUser_firstNameNull() {
    assertThatThrownBy { authUserService.amendUser("userme", null, "last") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field firstName with reason: required")
  }

  @Test
  fun amendUser_firstNameLessThan() {
    assertThatThrownBy { authUserService.amendUser("userme", "hello<input", "last") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field firstName with reason: invalid")
  }

  @Test
  fun amendUser_firstNameGreaterThan() {
    assertThatThrownBy { authUserService.amendUser("userme", "helloinput>", "last") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field firstName with reason: invalid")
  }

  @Test
  fun amendUser_lastNameBlank() {
    assertThatThrownBy { authUserService.amendUser("userme", "first", "  ") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field lastName with reason: required")
  }

  @Test
  fun amendUser_lastNameNull() {
    assertThatThrownBy { authUserService.amendUser("userme", "first", null) }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field lastName with reason: required")
  }

  @Test
  fun amendUser_lastNameLessThan() {
    assertThatThrownBy { authUserService.amendUser("userme", "last", "hello<input") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field lastName with reason: invalid")
  }

  @Test
  fun amendUser_lastNameGreaterThan() {
    assertThatThrownBy { authUserService.amendUser("userme", "last", "helloinput>") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field lastName with reason: invalid")
  }

  @Test
  fun amendUser_firstNameMaxLength() {
    assertThatThrownBy { authUserService.amendUser("userme", "ThisFirstNameIsMoreThanFiftyCharactersInLengthAndInvalid", "last") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field firstName with reason: maxlength")
  }

  @Test
  fun amendUser_lastNameLength() {
    assertThatThrownBy { authUserService.amendUser("userme", "se", "x") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field lastName with reason: length")
  }

  @Test
  fun amendUser_lastNameMaxLength() {
    assertThatThrownBy { authUserService.amendUser("userme", "se", "ThisLastNameIsMoreThanFiftyCharactersInLengthAndInvalid") }.isInstanceOf(CreateUserException::class.java).hasMessage("Create user failed for field lastName with reason: maxlength")
  }

  @Test
  fun amendUser_checkPerson() {
    val user = User.builder().username("me").person(Person("old", "name")).build()
    whenever(userRepository.findByUsernameAndSource(anyString(), any())).thenReturn(Optional.of(user))
    authUserService.amendUser("user", "first", "last")
    assertThat(user.person).isEqualTo(Person("first", "last"))
  }

  @Test
  fun amendUser_trimPerson() {
    val user = User.builder().username("me").person(Person("old", "name")).build()
    whenever(userRepository.findByUsernameAndSource(anyString(), any())).thenReturn(Optional.of(user))
    authUserService.amendUser("user", "  first  ", "   last ")
    assertThat(user.person).isEqualTo(Person("first", "last"))
  }

  @Test
  fun amendUser_checkRepositoryCall() {
    val user = User.builder().username("me").person(Person("old", "name")).build()
    whenever(userRepository.findByUsernameAndSource(anyString(), any())).thenReturn(Optional.of(user))
    authUserService.amendUser("user", "first", "last")
    verify(userRepository).findByUsernameAndSource("user", auth)
  }

  private val staffUserAccountForBob: UserPersonDetails
    get() {
      val staffUserAccount = NomisUserPersonDetails()
      val staff = Staff()
      staff.firstName = "bOb"
      staff.status = "ACTIVE"
      staffUserAccount.staff = staff
      val detail = AccountDetail("user", "OPEN", "profile", null)
      staffUserAccount.accountDetail = detail
      staffUserAccount.username = "bob"
      return staffUserAccount
    }

  companion object {
    private val PRINCIPAL: Authentication = UsernamePasswordAuthenticationToken("bob", "pass")
    private val GRANTED_AUTHORITY_SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}

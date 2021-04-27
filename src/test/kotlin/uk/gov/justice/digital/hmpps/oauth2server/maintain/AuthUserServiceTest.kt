@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.maintain

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserFilter.Status
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createOptionalSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetailsHelper.Companion.createSampleNomisUser
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.LinkEmailAndUsername
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientApi
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException

class AuthUserServiceTest {
  private val userRepository: UserRepository = mock()
  private val notificationClient: NotificationClientApi = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val authUserGroupService: AuthUserGroupService = mock()
  private val maintainUserCheck: MaintainUserCheck = mock()
  private val passwordEncoder: PasswordEncoder = mock()
  private val oauthServiceRepository: OauthServiceRepository = mock()
  private var authUserService = AuthUserService(
    userRepository,
    notificationClient,
    telemetryClient,
    verifyEmailService,
    authUserGroupService,
    maintainUserCheck,
    passwordEncoder,
    oauthServiceRepository,
    "licences",
    90,
    10
  )

  @BeforeEach
  fun setUp() {
    mockServiceOfNameWithSupportLink("NOMIS", "nomis_support_link")
  }

  @Nested
  inner class amendUserEmail {
    @Test
    fun emailValidation() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createOptionalSampleUser())
      doThrow(VerifyEmailException("reason")).whenever(verifyEmailService)
        .validateEmailAddress(anyString(), eq(EmailType.PRIMARY))
      assertThatThrownBy {
        authUserService.amendUserEmail(
          "userme",
          "email",
          "url?token=",
          "bob",
          PRINCIPAL.authorities,
          EmailType.PRIMARY
        )
      }.isInstanceOf(VerifyEmailException::class.java).hasMessage("Verify email failed with reason: reason")
      verify(verifyEmailService).validateEmailAddress("email", EmailType.PRIMARY)
    }

    @Test
    fun `manager not allowed to maintain user`() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createOptionalSampleUser())
      doThrow(AuthUserGroupRelationshipException("user", "reason")).whenever(maintainUserCheck)
        .ensureUserLoggedInUserRelationship(anyString(), any(), any())
      assertThatThrownBy {
        authUserService.amendUserEmail(
          "userme",
          "email",
          "url?token=",
          "bob",
          PRINCIPAL.authorities,
          EmailType.PRIMARY
        )
      }.isInstanceOf(AuthUserGroupRelationshipException::class.java)
        .hasMessage("Unable to maintain user: user with reason: reason")
    }

    @Test
    fun successLinkReturned() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createOptionalSampleUser())
      val link =
        authUserService.amendUserEmail("userme", "email", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
      assertThat(link).startsWith("url?token=").hasSize("url?token=".length + 36)
    }

    @Test
    fun trackSuccess() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createOptionalSampleUser())
      authUserService.amendUserEmail("userme", "email", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
      verify(telemetryClient).trackEvent(
        "AuthUserAmendSuccess",
        mapOf("username" to "someuser", "admin" to "bob"),
        null
      )
    }

    @Test
    fun saveTokenRepository() {
      val user = createOptionalSampleUser()
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(user)
      val link =
        authUserService.amendUserEmail("userme", "email", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
      val userToken = user.orElseThrow().tokens.stream().findFirst().orElseThrow()
      assertThat(userToken.tokenType).isEqualTo(RESET)
      assertThat(userToken.token).isEqualTo(link.substring("url?token=".length))
      assertThat(userToken.tokenExpiry).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8))
    }

    @Test
    fun `createUserByEmail first name does not meet minimum length`() {
      assertThatThrownBy {
        authUserService.createUserByEmail(
          "email",
          "s",
          "last",
          null,
          "url",
          "bob",
          GRANTED_AUTHORITY_SUPER_USER
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field firstName with reason: length")
    }

    @Test
    fun `createUserByEmail first Name exceeds max length`() {
      assertThatThrownBy {
        authUserService.createUserByEmail(
          "email",
          "ThisFirstNameIsMoreThanFiftyCharactersInLengthAndInvalid",
          "last",
          null,
          "url",
          "bob",
          GRANTED_AUTHORITY_SUPER_USER
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field firstName with reason: maxlength")
    }

    @Test
    fun `createUserByEmail last name does not meet minimum length`() {
      assertThatThrownBy {
        authUserService.createUserByEmail(
          "email",
          "se",
          "x",
          null,
          "url",
          "bob",
          GRANTED_AUTHORITY_SUPER_USER
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field lastName with reason: length")
    }

    @Test
    fun `createUserByEmail last name exceeds max lengh`() {
      assertThatThrownBy {
        authUserService.createUserByEmail(
          "email",
          "se",
          "ThisLastNameIsMoreThanFiftyCharactersInLengthAndInvalid",
          null,
          "url",
          "bob",
          GRANTED_AUTHORITY_SUPER_USER
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field lastName with reason: maxlength")
    }

    @Test
    fun `createUserByEmail fails email validation`() {
      doThrow(VerifyEmailException("reason")).whenever(verifyEmailService)
        .validateEmailAddress(anyString(), eq(EmailType.PRIMARY))
      assertThatThrownBy {
        authUserService.createUserByEmail(
          "email",
          "se",
          "xx",
          null,
          "url",
          "bob",
          GRANTED_AUTHORITY_SUPER_USER
        )
      }.isInstanceOf(VerifyEmailException::class.java).hasMessage("Verify email failed with reason: reason")
      verify(verifyEmailService).validateEmailAddress("email", EmailType.PRIMARY)
    }

    @Test
    fun `createUserByEmail returns success link`() {
      val link =
        authUserService.createUserByEmail(
          "email",
          "se",
          "xx",
          null,
          "url?token=",
          "bob",
          GRANTED_AUTHORITY_SUPER_USER
        )
      assertThat(link).startsWith("url?token=").hasSize("url?token=".length + 36)
    }

    @Test
    fun `createUserByEmail track success`() {
      authUserService.createUserByEmail(
        "example@justice.gov.uk",
        "se",
        "xx",
        null,
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(telemetryClient).trackEvent(
        "AuthUserCreateSuccess",
        mapOf("username" to "EXAMPLE@JUSTICE.GOV.UK", "admin" to "bob"),
        null
      )
    }

    @Test
    fun `createUserByEmail save user repository token`() {
      val link =
        authUserService.createUserByEmail(
          "email",
          "se",
          "xx",
          null,
          "url?token=",
          "bob",
          GRANTED_AUTHORITY_SUPER_USER
        )
      verify(userRepository).save<User>(
        check {
          val userToken = it.tokens.stream().findFirst().orElseThrow()
          assertThat(userToken.tokenType).isEqualTo(RESET)
          assertThat(userToken.token).isEqualTo(link.substring("url?token=".length))
          assertThat(userToken.tokenExpiry).isBetween(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(8))
        }
      )
    }

    @Test
    fun `createUser save user repository`() {
      authUserService.createUserByEmail(
        "example@justice.gov.uk",
        "first",
        "last",
        null,
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(userRepository).save<User>(
        check {
          with(it) {
            assertThat(name).isEqualTo("first last")
            assertThat(email).isEqualTo("example@justice.gov.uk")
            assertThat(username).isEqualTo("EXAMPLE@JUSTICE.GOV.UK")
            assertThat(password).isNull()
            assertThat(isMaster).isTrue()
            assertThat(verified).isFalse()
            assertThat(isCredentialsNonExpired).isFalse()
            assertThat(authorities).isEmpty()
          }
        }
      )
    }

    @Test
    fun `createUserByEmail trim name`() {
      authUserService.createUserByEmail(
        "eMail",
        "first  ",
        "  last ",
        null,
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(userRepository).save<User>(
        check {
          assertThat(it.name).isEqualTo("first last")
        }
      )
    }

    @Test
    fun `createUserByEmail email input is formatted`() {
      authUserService.createUserByEmail(
        "    SARAH.o’connor@gov.uk",
        "first",
        "last",
        null,
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(userRepository).save<User>(
        check {
          assertThat(it.email).isEqualTo("sarah.o'connor@gov.uk")
        }
      )
    }

    @Test
    fun `createUserByEmail groups are set`() {
      whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(
        listOf(
          Group(
            "SITE_1_GROUP_1",
            "desc"
          ),
          Group(
            "SITE_1_GROUP_2",
            "desc"
          ),
        )
      )
      authUserService.createUserByEmail(
        "eMail",
        "first",
        "last",
        setOf("SITE_1_GROUP_1", "SITE_1_GROUP_2", "SITE_1_GROUP_3"),
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(userRepository).save<User>(
        check {
          assertThat(it.groups).extracting<String> { it.groupCode }.containsOnly("SITE_1_GROUP_1", "SITE_1_GROUP_2")
        }
      )
    }

    @Test
    fun `createUserByEmail has no roles`() {
      whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(
        listOf(
          Group(
            "SITE_1_GROUP_1",
            "desc"
          )
        )
      )
      authUserService.createUserByEmail(
        "eMail",
        "first",
        "last",
        setOf("SITE_1_GROUP_1"),
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(userRepository).save<User>(
        check {
          assertThat(it.authorities).isEmpty()
        }
      )
    }

    @Test
    fun `createUserByEmail roles are set`() {
      val group = Group("SITE_1_GROUP_1", "desc")
      group.assignableRoles.add(GroupAssignableRole(Authority("AUTH_AUTO", "Auth Name"), group, true))
      group.assignableRoles.add(GroupAssignableRole(Authority("AUTH_MANUAL", "Auth Name"), group, false))

      whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(listOf(group))
      authUserService.createUserByEmail(
        "eMail",
        "first",
        "last",
        setOf("SITE_1_GROUP_1"),
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(userRepository).save<User>(
        check {
          assertThat(it.authorities).extracting<String> { it.roleCode }.containsOnly("AUTH_AUTO")
        }
      )
    }

    @Test
    fun `createUserByEmail fails if group not found`() {
      whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(
        listOf(
          Group(
            "OTHER_GROUP",
            "desc"
          )
        )
      )
      assertThatThrownBy {
        authUserService.createUserByEmail(
          "eMail",
          "first",
          "last",
          setOf("SITE_2_GROUP_1"),
          "url?token=",
          "bob",
          GRANTED_AUTHORITY_SUPER_USER
        )
      }
        .isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field groupCode with reason: notfound")
    }

    @Test
    fun `createUserByEmail fails if group is missing`() {
      assertThatThrownBy {
        authUserService.createUserByEmail(
          "eMail",
          "first",
          "last",
          emptySet(),
          "url?token=",
          "bob",
          setOf()
        )
      }
        .isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field groupCode with reason: missing")
    }

    @Test
    fun `createUserByEmail calls Notify`() {
      val link = authUserService.createUserByEmail(
        "email",
        "first",
        "last",
        null,
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(notificationClient).sendEmail(
        "licences",
        "email",
        mapOf(
          "resetLink" to link,
          "firstName" to "first last",
          "fullName" to "first last",
          "supportLink" to "nomis_support_link"
        ),
        null
      )
    }

    @Test
    fun `createUserByEmail pecs user group support link`() {
      mockServiceOfNameWithSupportLink("BOOK_MOVE", "book_move_support_link")
      whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(
        listOf(
          Group(
            "PECS_GROUP",
            "desc"
          )
        )
      )
      authUserService.createUserByEmail(
        "eMail",
        "first",
        "last",
        setOf("PECS_GROUP"),
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(notificationClient).sendEmail(
        anyString(),
        anyString(),
        check {
          assertThat(it["supportLink"]).isEqualTo("book_move_support_link")
        },
        isNull()
      )
    }

    @Test
    fun `createUserByEmail support links for no groups`() {
      authUserService.createUserByEmail(
        "eMail",
        "first",
        "last",
        emptySet(),
        "url?token=",
        "bob",
        GRANTED_AUTHORITY_SUPER_USER
      )
      verify(notificationClient).sendEmail(
        anyString(),
        anyString(),
        check {
          assertThat(it["supportLink"]).isEqualTo("nomis_support_link")
        },
        isNull()
      )
    }

    @Test
    fun saveEmailRepository() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createOptionalSampleUser())
      authUserService.amendUserEmail("userMe", "eMail", "url?token=", "bob", PRINCIPAL.authorities, EmailType.PRIMARY)
      verify(userRepository).save<User>(
        check {
          assertThat(it.email).isEqualTo("email")
        }
      )
    }

    @Test
    fun formatEmailInput() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createOptionalSampleUser())
      authUserService.amendUserEmail(
        "userMe",
        "    SARAH.o’connor@gov.uk",
        "url?token=",
        "bob",
        PRINCIPAL.authorities,
        EmailType.PRIMARY
      )
      verify(userRepository).save<User>(
        check {
          assertThat(it.email).isEqualTo("sarah.o'connor@gov.uk")
        }
      )
    }

    @Test
    fun pecsUserGroupSupportLink() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups("PECS_GROUP")))
      mockServiceOfNameWithSupportLink("BOOK_MOVE", "book_move_support_link")
      authUserService.amendUserEmail(
        "ANY_USER_NAME",
        "ANY_USER-EMAIL",
        "ANY_URL",
        "ANY_ADMIN",
        GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY
      )
      verify(notificationClient).sendEmail(
        anyString(),
        anyString(),
        check {
          assertThat(it["supportLink"]).isEqualTo("book_move_support_link")
        },
        isNull()
      )
    }

    @Test
    fun nonPecsUserGroupSupportLink() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups("NON_PECS_GROUP")))
      authUserService.amendUserEmail(
        "ANY_USER_NAME",
        "ANY_USER-EMAIL",
        "ANY_URL",
        "ANY_ADMIN",
        GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY
      )
      verify(notificationClient).sendEmail(
        anyString(),
        anyString(),
        check {
          assertThat(it["supportLink"]).isEqualTo("nomis_support_link")
        },
        isNull()
      )
    }

    @Test
    fun onePecsGroupOfManySupportLink() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(
        Optional.of(
          userOfGroups(
            "NON_PECS_GROUP",
            "PECS_GROUP"
          )
        )
      )
      mockServiceOfNameWithSupportLink("BOOK_MOVE", "book_move_support_link")
      authUserService.amendUserEmail(
        "ANY_USER_NAME",
        "ANY_USER-EMAIL",
        "ANY_URL",
        "ANY_ADMIN",
        GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY
      )
      verify(notificationClient).sendEmail(
        anyString(),
        anyString(),
        check {
          assertThat(it["supportLink"]).isEqualTo("book_move_support_link")
        },
        isNull()
      )
    }

    @Test
    fun noGroupSupportLink() {
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userOfGroups()))
      authUserService.amendUserEmail(
        "ANY_USER_NAME",
        "ANY_USER-EMAIL",
        "ANY_URL",
        "ANY_ADMIN",
        GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY
      )
      verify(notificationClient).sendEmail(
        anyString(),
        anyString(),
        check {
          assertThat(it["supportLink"]).isEqualTo("nomis_support_link")
        },
        isNull()
      )
    }

    @Test
    fun `never logged in sends initial email`() {
      val userUnverifiedEmail = createSampleUser(username = "SOME_USER_NAME")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userUnverifiedEmail))
      authUserService.amendUserEmail(
        "SOME_USER_NAME",
        "some_user_email@gov.uk",
        "ANY_HOST/initial-password?token=SOME_TOKEN",
        "ANY_ADMIN",
        GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY
      )
      verify(notificationClient).sendEmail(anyString(), anyString(), any(), isNull())
      verify(verifyEmailService, never()).changeEmailAndRequestVerification(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq(EmailType.PRIMARY)
      )
    }

    @Test
    fun `has logged in requests verification`() {
      val userVerifiedEmail =
        createSampleUser(
          username = "SOME_USER_NAME",
          firstName = "first",
          lastName = "last",
          verified = true,
          password = "isset"
        )
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userVerifiedEmail))
      whenever(
        verifyEmailService.changeEmailAndRequestVerification(
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          any(),
        )
      ).thenReturn(LinkEmailAndUsername("SOME_VERIFY_LINK", "newemail@justice.gov.uk", "SOME_USER_NAME"))
      authUserService.amendUserEmail(
        "SOME_user_NAME",
        "some_user_email@gov.uk",
        "SOME_HOST/initial-password?token=SOME_TOKEN",
        "ANY_ADMIN",
        GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY
      )
      verify(verifyEmailService).changeEmailAndRequestVerification(
        "SOME_USER_NAME",
        "some_user_email@gov.uk",
        "first",
        "first last",
        "SOME_HOST/verify-email-confirm?token=SOME_TOKEN",
        EmailType.PRIMARY
      )
      verifyZeroInteractions(notificationClient)
    }

    @Test
    fun `never logged in changes email address if same as username`() {
      whenever(
        verifyEmailService.changeEmailAndRequestVerification(
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          any(),
        )
      ).thenReturn(LinkEmailAndUsername("SOME_VERIFY_LINK", "newemail@justice.gov.uk", "SOME_EXISTING_EMAIL@GOV.UK"))
      val userVerifiedEmail =
        createSampleUser(username = "SOME_EXISTING_EMAIL@GOV.UK", verified = true, email = "some_existing_email@gov.uk")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userVerifiedEmail))
      authUserService.amendUserEmail(
        "SOME_EXISTING_email@gov.uk",
        "some_user_email@gov.uk",
        "ANY_HOST/initial-password?token=SOME_TOKEN",
        "ANY_ADMIN",
        GRANTED_AUTHORITY_SUPER_USER,
        EmailType.PRIMARY
      )
      assertThat(userVerifiedEmail.username).isEqualTo("SOME_USER_EMAIL@GOV.UK")
      assertThat(userVerifiedEmail.email).isEqualTo("some_user_email@gov.uk")
      assertThat(userVerifiedEmail.verified).isFalse
    }

    @Test
    fun `never logged in can't change email to same as existing user`() {
      whenever(
        verifyEmailService.changeEmailAndRequestVerification(
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          anyString(),
          any(),
        )
      ).thenReturn(LinkEmailAndUsername("SOME_VERIFY_LINK", "newemail@justice.gov.uk", "SOME_EXISTING_EMAIL@GOV.UK"))
      val userVerifiedEmail =
        createSampleUser(username = "SOME_EXISTING_EMAIL@GOV.UK", verified = true, email = "some_existing_email@gov.uk")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(userVerifiedEmail))
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(userVerifiedEmail))
      assertThatThrownBy {
        authUserService.amendUserEmail(
          "SOME_EXISTING_email@gov.uk",
          "some_user_email@gov.uk",
          "ANY_HOST/initial-password?token=SOME_TOKEN",
          "ANY_ADMIN",
          GRANTED_AUTHORITY_SUPER_USER,
          EmailType.PRIMARY
        )
      }.hasMessage("Verify email failed with reason: duplicate")
    }
  }

  private fun userOfGroups(vararg groupList: String): User {
    val groups = groupList.map { Group(it, "any desc") }.toSet()
    return createSampleUser(
      username = "ANY ANY",
      groups = groups,
      email = "ANY_EMAIL",
      firstName = "ANY_FIRST_NAME",
      lastName = "ANY_LAST_NAME"
    )
  }

  private fun mockServiceOfNameWithSupportLink(serviceCode: String, supportLink: String) {
    val service = Service(serviceCode, "service", "service", "ANY_ROLES", "ANY_URL", true, supportLink)
    whenever(oauthServiceRepository.findById(serviceCode)).thenReturn(Optional.of(service))
  }

  @Test
  fun authUserByUsername() {
    val createdUser = createOptionalSampleUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createdUser)
    val user = authUserService.getAuthUserByUsername("   bob   ")
    assertThat(user).isPresent.get().isEqualTo(createdUser.orElseThrow())
    verify(userRepository).findByUsernameAndMasterIsTrue("BOB")
  }

  @Test
  fun findByEmailAndMasterIsTrue() {
    whenever(userRepository.findByEmailAndMasterIsTrueOrderByUsername(anyString())).thenReturn(
      listOf(
        createSampleUser(
          username = "someuser"
        )
      )
    )
    val user = authUserService.findAuthUsersByEmail("  bob  ")
    assertThat(user).extracting<String> { it.username }.containsOnly("someuser")
  }

  @Test
  fun findAuthUsersByEmail_formatEmailAddress() {
    whenever(userRepository.findByEmailAndMasterIsTrueOrderByUsername(anyString())).thenReturn(
      listOf(
        createSampleUser(
          username = "someuser"
        )
      )
    )
    authUserService.findAuthUsersByEmail("  some.u’ser@SOMEwhere  ")
    verify(userRepository).findByEmailAndMasterIsTrueOrderByUsername("some.u'ser@somewhere")
  }

  @Test
  fun enableUser_superUser() {
    val optionalUser = createOptionalSampleUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.enableUser("user", "admin", SUPER_USER)
    assertThat(optionalUser).get().extracting { it.isEnabled }.isEqualTo(true)
    verify(userRepository).save(optionalUser.orElseThrow())
  }

  @Test
  fun enableUser_invalidGroup_GroupManager() {
    val optionalUser = createOptionalSampleUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    doThrow(AuthUserGroupRelationshipException("someuser", "User not with your groups")).whenever(maintainUserCheck)
      .ensureUserLoggedInUserRelationship(anyString(), anyCollection(), any())
    assertThatThrownBy { authUserService.enableUser("someuser", "admin", GROUP_MANAGER) }.isInstanceOf(
      AuthUserGroupRelationshipException::class.java
    ).hasMessage("Unable to maintain user: someuser with reason: User not with your groups")
  }

  @Test
  fun enableUser_validGroup_groupManager() {
    val user = createSampleUser(
      username = "user",
      groups = setOf(Group("group", "desc"), Group("group2", "desc")),
      authorities = setOf(Authority("JOE", "bloggs"))
    )
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
      .thenReturn(Optional.of(user))
    authUserService.enableUser("user", "admin", GROUP_MANAGER)
    assertThat(user).extracting { it.isEnabled }.isEqualTo(true)
    verify(userRepository).save(user)
  }

  @Test
  fun enableUser_NotFound() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy {
      authUserService.enableUser(
        "user",
        "admin",
        SUPER_USER
      )
    }.isInstanceOf(EntityNotFoundException::class.java).hasMessageContaining("username user")
  }

  @Test
  fun enableUser_trackEvent() {
    val optionalUser = createOptionalSampleUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.enableUser("someuser", "someadmin", SUPER_USER)
    verify(telemetryClient).trackEvent(
      "AuthUserChangeEnabled",
      mapOf("username" to "someuser", "admin" to "someadmin", "enabled" to "true"),
      null
    )
  }

  @Test
  fun enableUser_setLastLoggedIn() {
    val optionalUser = createOptionalSampleUser()
    val user = optionalUser.orElseThrow()
    val tooLongAgo = LocalDateTime.now().minusDays(95)
    user.lastLoggedIn = tooLongAgo
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.enableUser("someuser", "someadmin", SUPER_USER)
    assertThat(user.lastLoggedIn).isBetween(LocalDateTime.now().minusDays(84), LocalDateTime.now().minusDays(82))
  }

  @Test
  fun enableUser_leaveLastLoggedInAlone() {
    val optionalUser = createOptionalSampleUser()
    val user = optionalUser.orElseThrow()
    val fiveDaysAgo = LocalDateTime.now().minusDays(5)
    user.lastLoggedIn = fiveDaysAgo
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.enableUser("someuser", "someadmin", SUPER_USER)
    assertThat(user.lastLoggedIn).isEqualTo(fiveDaysAgo)
  }

  @Test
  fun disableUser_superUser() {
    val optionalUser = createOptionalSampleUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.disableUser("user", "admin", SUPER_USER)
    assertThat(optionalUser).get().extracting { it.isEnabled }.isEqualTo(false)
    verify(userRepository).save(optionalUser.orElseThrow())
  }

  @Test
  fun disableUser_invalidGroup_GroupManager() {
    val optionalUser = createOptionalSampleUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    doThrow(AuthUserGroupRelationshipException("someuser", "User not with your groups")).whenever(maintainUserCheck)
      .ensureUserLoggedInUserRelationship(anyString(), anyCollection(), any())
    assertThatThrownBy { authUserService.disableUser("someuser", "admin", GROUP_MANAGER) }.isInstanceOf(
      AuthUserGroupRelationshipException::class.java
    ).hasMessage("Unable to maintain user: someuser with reason: User not with your groups")
  }

  @Test
  fun disableUser_validGroup_groupManager() {
    val group1 = Group("group", "desc")
    val user = createSampleUser(
      username = "user",
      groups = setOf(group1, Group("group2", "desc")),
      enabled = true,
      authorities = setOf(Authority("JOE", "bloggs"))
    )
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
      .thenReturn(Optional.of(user))
    authUserService.disableUser("user", "admin", GROUP_MANAGER)
    assertThat(user).extracting { it.isEnabled }.isEqualTo(false)
    verify(userRepository).save(user)
  }

  @Test
  fun disableUser_trackEvent() {
    val optionalUser = createOptionalSampleUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUser)
    authUserService.disableUser("someuser", "someadmin", SUPER_USER)
    verify(telemetryClient).trackEvent(
      "AuthUserChangeEnabled",
      mapOf("username" to "someuser", "admin" to "someadmin", "enabled" to "false"),
      null
    )
  }

  @Test
  fun disableUser_notFound() {
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy {
      authUserService.disableUser(
        "user",
        "admin",
        SUPER_USER
      )
    }.isInstanceOf(EntityNotFoundException::class.java).hasMessageContaining("username user")
  }

  @Nested
  inner class findAuthUsers {
    @Test
    fun findAuthUsers() {
      whenever(userRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      authUserService.findAuthUsers(
        "somename ",
        listOf("somerole"),
        listOf("somegroup"),
        unpaged,
        "bob",
        GRANTED_AUTHORITY_SUPER_USER,
        Status.ALL,
      )
      verify(userRepository).findAll(
        check {
          assertThat(it).extracting("name", "roleCodes", "groupCodes")
            .containsExactly("somename", listOf("somerole"), listOf("somegroup"))
        },
        eq(unpaged)
      )
    }

    @Test
    fun `passes multiple auth sources to the repository specification`() {
      whenever(userRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      authUserService.findAuthUsers(
        "somename ",
        null,
        null,
        unpaged,
        "bob",
        GRANTED_AUTHORITY_SUPER_USER,
        Status.ACTIVE,
        listOf(auth, nomis, delius),
      )
      verify(userRepository).findAll(
        check {
          assertThat(it).extracting("authSources").asList().containsAll(listOf(auth, nomis, delius))
        },
        eq(unpaged)
      )
    }

    @Test
    fun `passes a default auth source through to the repository specification`() {
      whenever(userRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      authUserService.findAuthUsers(
        "somename ",
        null,
        null,
        unpaged,
        "bob",
        GRANTED_AUTHORITY_SUPER_USER,
        Status.ACTIVE,
      )
      verify(userRepository).findAll(
        check {
          assertThat(it).extracting("authSources").asList().containsOnly(auth)
        },
        eq(unpaged)
      )
    }

    @Test
    fun `passes status through to filter`() {
      whenever(userRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      authUserService.findAuthUsers(
        "somename ",
        listOf("somerole"),
        listOf("somegroup"),
        unpaged,
        "bob",
        GRANTED_AUTHORITY_SUPER_USER,
        Status.ACTIVE,
      )
      verify(userRepository).findAll(
        check {
          assertThat(it).extracting("status").isEqualTo(Status.ACTIVE)
        },
        eq(unpaged)
      )
    }

    @Test
    fun `adds all group manager groups if no group specified`() {
      whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(
        listOf(
          Group("SITE_1_GROUP_1", "desc"),
          Group("SITE_1_GROUP_2", "desc"),
        )
      )
      whenever(userRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      authUserService.findAuthUsers(
        "somename ",
        listOf("somerole"),
        null,
        unpaged,
        "bob",
        GROUP_MANAGER,
        Status.ALL,
      )
      verify(userRepository).findAll(
        check {
          assertThat(it).extracting("name", "roleCodes", "groupCodes")
            .containsExactly("somename", listOf("somerole"), listOf("SITE_1_GROUP_1", "SITE_1_GROUP_2"))
        },
        eq(unpaged)
      )
    }

    @Test
    fun `filters out groups that group manager isn't a member of`() {
      whenever(authUserGroupService.getAssignableGroups(anyString(), any())).thenReturn(
        listOf(
          Group("SITE_1_GROUP_1", "desc"),
          Group("SITE_1_GROUP_2", "desc"),
        )
      )
      whenever(userRepository.findAll(any(), any<Pageable>())).thenReturn(Page.empty())
      val unpaged = Pageable.unpaged()
      authUserService.findAuthUsers(
        "somename ",
        listOf("somerole"),
        listOf("somegroup", "SITE_1_GROUP_1"),
        unpaged,
        "bob",
        GROUP_MANAGER,
        Status.ALL,
      )
      verify(userRepository).findAll(
        check {
          assertThat(it).extracting("name", "roleCodes", "groupCodes")
            .containsExactly("somename", listOf("somerole"), listOf("SITE_1_GROUP_1"))
        },
        eq(unpaged)
      )
    }
  }

  @Test
  fun lockUser_alreadyExists() {
    val user = createSampleUser(username = "user")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    authUserService.lockUser(user)
    assertThat(user.locked).isTrue
    verify(userRepository).save(user)
  }

  @Test
  fun lockUser_newUser() {
    val user = staffUserAccountForBob
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    authUserService.lockUser(user)
    verify(userRepository).save<User>(
      check {
        assertThat(it.locked).isTrue
        assertThat(it.username).isEqualTo("bob")
        assertThat(it.source).isEqualTo(nomis)
      }
    )
  }

  @Test
  fun unlockUser_alreadyExists() {
    val user = createSampleUser(username = "user", locked = true)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    authUserService.unlockUser(user)
    assertThat(user.locked).isFalse
    assertThat(user.verified).isTrue
    verify(userRepository).save(user)
  }

  @Test
  fun unlockUser_newUser() {
    val user = staffUserAccountForBob
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    authUserService.unlockUser(user)
    verify(userRepository).save<User>(
      check {
        assertThat(it.locked).isFalse
        assertThat(it.verified).isTrue
        assertThat(it.username).isEqualTo("bob")
        assertThat(it.source).isEqualTo(nomis)
      }
    )
  }

  @Test
  fun changePassword() {
    val user = createSampleUser(username = "user")
    whenever(passwordEncoder.encode(anyString())).thenReturn("hashedpassword")
    authUserService.changePassword(user, "pass")
    assertThat(user.password).isEqualTo("hashedpassword")
    assertThat(user.passwordExpiry).isAfterOrEqualTo(LocalDateTime.now().plusDays(9))
    verify(passwordEncoder).encode("pass")
  }

  @Test
  fun changePassword_PasswordSameAsCurrent() {
    val user = createSampleUser(username = "user", password = "oldencryptedpassword")
    whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)
    assertThatThrownBy {
      authUserService.changePassword(
        user,
        "pass"
      )
    }.isInstanceOf(ReusedPasswordException::class.java)
    verify(passwordEncoder).matches("pass", "oldencryptedpassword")
  }

  @Nested
  inner class amendUser {
    @Test
    fun firstNameLength() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "s",
          "last"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field firstName with reason: length")
    }

    @Test
    fun firstNameBlank() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "  ",
          "last"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field firstName with reason: required")
    }

    @Test
    fun firstNameNull() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          null,
          "last"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field firstName with reason: required")
    }

    @Test
    fun firstNameLessThan() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "hello<input",
          "last"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field firstName with reason: invalid")
    }

    @Test
    fun firstNameGreaterThan() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "helloinput>",
          "last"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field firstName with reason: invalid")
    }

    @Test
    fun lastNameBlank() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "first",
          "  "
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field lastName with reason: required")
    }

    @Test
    fun lastNameNull() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "first",
          null
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field lastName with reason: required")
    }

    @Test
    fun lastNameLessThan() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "last",
          "hello<input"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field lastName with reason: invalid")
    }

    @Test
    fun lastNameGreaterThan() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "last",
          "helloinput>"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field lastName with reason: invalid")
    }

    @Test
    fun firstNameMaxLength() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "ThisFirstNameIsMoreThanFiftyCharactersInLengthAndInvalid",
          "last"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field firstName with reason: maxlength")
    }

    @Test
    fun lastNameLength() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "se",
          "x"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field lastName with reason: length")
    }

    @Test
    fun lastNameMaxLength() {
      assertThatThrownBy {
        authUserService.amendUser(
          "userme",
          "se",
          "ThisLastNameIsMoreThanFiftyCharactersInLengthAndInvalid"
        )
      }.isInstanceOf(CreateUserException::class.java)
        .hasMessage("Create user failed for field lastName with reason: maxlength")
    }

    @Test
    fun checkPerson() {
      val user = createSampleUser(username = "me", firstName = "old", lastName = "name")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      authUserService.amendUser("user", "first", "last")
      assertThat(user.person).isEqualTo(Person("first", "last"))
    }

    @Test
    fun trimPerson() {
      val user = createSampleUser(username = "me", firstName = "old", lastName = "name")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      authUserService.amendUser("user", "  first  ", "   last ")
      assertThat(user.person).isEqualTo(Person("first", "last"))
    }

    @Test
    fun checkRepositoryCall() {
      val user = createSampleUser(username = "me", firstName = "old", lastName = "name")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
      authUserService.amendUser("user", "first", "last")
      verify(userRepository).findByUsernameAndMasterIsTrue("user")
    }
  }

  @Nested
  inner class useEmailAsUsername {
    @Test
    fun `switch username to email address`() {
      val user = createSampleUser(username = "me", firstName = "old", lastName = "name", email = "me@me.com")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
        .thenReturn(Optional.empty())
      assertThat(authUserService.useEmailAsUsername("user")).isEqualTo("me@me.com")
      assertThat(user.username).isEqualTo("ME@ME.COM")
    }

    @Test
    fun `switch username to email address sends event`() {
      val user = createSampleUser(username = "me", firstName = "old", lastName = "name", email = "me@me.com")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
        .thenReturn(Optional.empty())
      assertThat(authUserService.useEmailAsUsername("user")).isEqualTo("me@me.com")
      verify(telemetryClient).trackEvent(
        "AuthUserChangeUsername",
        mapOf("username" to "ME@ME.COM", "previous" to "user"),
        null
      )
    }

    @Test
    fun `can't switch username to email address if no email set`() {
      val user = createSampleUser(username = "me", firstName = "old", lastName = "name")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
        .thenReturn(Optional.empty())
      assertThat(authUserService.useEmailAsUsername("user")).isNull()
      assertThat(user.username).isEqualTo("me")
      verifyZeroInteractions(telemetryClient)
    }

    @Test
    fun `can't switch username to email address if using email already`() {
      val user = createSampleUser(username = "me@joe.com", firstName = "old", lastName = "name", email = "me@me.com")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
        .thenReturn(Optional.empty())
      assertThat(authUserService.useEmailAsUsername("user")).isNull()
      assertThat(user.username).isEqualTo("me@joe.com")
      verifyZeroInteractions(telemetryClient)
    }

    @Test
    fun `can't switch username to email address if email already taken`() {
      val user = createSampleUser(username = "me", firstName = "old", lastName = "name", email = "me@me.com")
      whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.of(user))
        .thenReturn(Optional.of(user))
      assertThat(authUserService.useEmailAsUsername("user")).isNull()
      assertThat(user.username).isEqualTo("me")
      verifyZeroInteractions(telemetryClient)
    }
  }

  private val staffUserAccountForBob: UserPersonDetails
    get() = createSampleNomisUser()

  companion object {
    private val PRINCIPAL: Authentication = UsernamePasswordAuthenticationToken("bob", "pass")
    private val GRANTED_AUTHORITY_SUPER_USER: Set<GrantedAuthority> =
      setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}

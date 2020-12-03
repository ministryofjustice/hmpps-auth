package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.LockedException
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetailsHelper.Companion.createSampleNomisUser
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Role
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.UserCaseloadRole
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.UserCaseloadRoleIdentity
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.delius
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.ResetPasswordException
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.util.Map.entry
import java.util.Optional

class ResetPasswordServiceTest {
  private val userRepository: UserRepository = mock()
  private val userTokenRepository: UserTokenRepository = mock()
  private val userService: UserService = mock()
  private val delegatingUserService: DelegatingUserService = mock()
  private val notificationClient: NotificationClientApi = mock()
  private val resetPasswordService = ResetPasswordServiceImpl(
    userRepository, userTokenRepository,
    userService, delegatingUserService, notificationClient,
    "resetTemplate", "resetUnavailableTemplate", "resetUnavailableEmailNotFoundTemplate", "reset-confirm"
  )

  @Nested
  inner class requestResetPassword {
    @Test
    fun requestResetPassword_noUserEmail() {
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.empty())
      val optional = resetPasswordService.requestResetPassword("user", "url")
      assertThat(optional).isEmpty
    }

    @Test
    fun requestResetPassword_noEmail() {
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(createSampleUser(username = "user")))
      val optional = resetPasswordService.requestResetPassword("user", "url")
      assertThat(optional).isEmpty
    }

    @Test
    fun requestResetPassword_noNomisUser() {
      val user = createSampleUser(username = "USER", email = "email", verified = true, source = nomis)
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.empty())
      val optional = resetPasswordService.requestResetPassword("user", "url")
      verify(notificationClient).sendEmail(
        eq("resetUnavailableTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(entry("firstName", "USER"), entry("fullName", "first last"))
        },
        isNull()
      )
      assertThat(optional).isEmpty
    }

    @Test
    fun requestResetPassword_inactive() {
      val user = createSampleUser(username = "someuser", email = "email", source = nomis, verified = true)
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val staffUserAccount =
        nomisUserPersonDetails("OPEN", Staff(firstName = "bOb", status = "inactive", lastName = "Smith", staffId = 5))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccount)
      val optional = resetPasswordService.requestResetPassword("user", "url")
      verify(notificationClient).sendEmail(
        eq("resetUnavailableTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(entry("firstName", "Bob"), entry("fullName", "Bob Smith"))
        },
        isNull()
      )
      assertThat(optional).isEmpty
    }

    @Test
    fun requestResetPassword_authLocked() {
      val user = createSampleUser(
        username = "someuser",
        email = "email",
        verified = true,
        locked = true,
        firstName = "Bob",
        lastName = "Smith",
        enabled = true
      )
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(user)

      val optionalLink = resetPasswordService.requestResetPassword("user", "url")
      verify(notificationClient).sendEmail(
        eq("resetTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(
            entry("firstName", "Bob"),
            entry("fullName", "Bob Smith"),
            entry("resetLink", optionalLink.get())
          )
        },
        isNull()
      )
      assertThat(optionalLink).isPresent
    }

    @Test
    fun requestResetPassword_notAuthLocked() {
      val user = createSampleUser(username = "someuser", email = "email", verified = true, source = nomis)
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val accountOptional = staffUserAccountLockedForBobOptional
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountLockedForBob)
      val optional = resetPasswordService.requestResetPassword("user", "url")
      verify(notificationClient).sendEmail(
        eq("resetUnavailableTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(entry("firstName", "Bob"), entry("fullName", "Bob Smith"))
        },
        isNull()
      )
      assertThat(optional).isEmpty
    }

    @Test
    fun requestResetPassword_userLocked() {
      val user = createSampleUser(username = "someuser", email = "email", source = nomis, verified = true)
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountExpiredLockedForBob)
      val optionalLink = resetPasswordService.requestResetPassword("user", "url")
      verify(notificationClient).sendEmail(
        eq("resetTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(
            entry("firstName", "Bob"),
            entry("fullName", "Bob Smith"),
            entry("resetLink", optionalLink.get())
          )
        },
        isNull()
      )
      assertThat(optionalLink).isPresent
    }

    @Test
    fun requestResetPassword_existingToken() {
      val user = createSampleUser(username = "someuser", email = "email", verified = true, locked = true)
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBobOptional)
      val existingUserToken = user.createToken(UserToken.TokenType.RESET)
      resetPasswordService.requestResetPassword("user", "url")
      assertThat(user.tokens).hasSize(1).extracting<String> { it.token }.containsExactly(existingUserToken.token)
    }

    @Test
    fun requestResetPassword_verifyToken() {
      val user = createSampleUser(
        username = "someuser",
        person = Person("Bob", "Smith"),
        email = "email",
        locked = true,
        source = nomis
      )
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBob)
      val optionalLink = resetPasswordService.requestResetPassword("user", "url")
      verify(notificationClient).sendEmail(
        eq("resetTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(
            entry("firstName", "Bob"),
            entry("fullName", "Bob Smith"),
            entry("resetLink", optionalLink.get())
          )
        },
        isNull()
      )
      assertThat(optionalLink).isPresent
    }

    @Test
    fun requestResetPassword_uppercaseUsername() {
      val user = createSampleUser(username = "SOMEUSER", email = "email", locked = true, source = nomis)
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBob)
      resetPasswordService.requestResetPassword("someuser", "url")
      verify(userRepository).findByUsername("SOMEUSER")
      verify(userService).findEnabledMasterUserPersonDetails("SOMEUSER")
    }

    @Test
    fun requestResetPassword_verifyNotification() {
      val user = createSampleUser(username = "someuser", email = "email", locked = true, source = nomis)
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBob)
      val linkOptional = resetPasswordService.requestResetPassword("user", "url")
      val value = user.tokens.stream().findFirst().orElseThrow()
      assertThat(linkOptional).get().isEqualTo(String.format("url-confirm?token=%s", value.token))
      assertThat(value.tokenType).isEqualTo(UserToken.TokenType.RESET)
      assertThat(value.user.email).isEqualTo("email")
    }

    @Test
    fun requestResetPassword_sendFailure() {
      val user = createSampleUser(username = "someuser", email = "email", locked = true, source = nomis)
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBob)
      whenever(notificationClient.sendEmail(anyString(), anyString(), anyMap<String, Any?>(), isNull())).thenThrow(
        NotificationClientException("message")
      )
      assertThatThrownBy {
        resetPasswordService.requestResetPassword(
          "user",
          "url"
        )
      }.hasMessageContaining("NotificationClientException: message")
    }

    @Test
    fun requestResetPassword_emailAddressNotFound() {
      whenever(userRepository.findByEmail(any())).thenReturn(emptyList())
      val optional = resetPasswordService.requestResetPassword("someuser@somewhere", "url")
      verify(notificationClient).sendEmail(
        eq("resetUnavailableEmailNotFoundTemplate"),
        eq("someuser@somewhere"),
        check {
          assertThat(it).isEmpty()
        },
        isNull()
      )
      assertThat(optional).isEmpty
    }

    @Test
    fun requestResetPassword_emailAddressNotFound_formatEmailInput() {
      whenever(userRepository.findByEmail(any())).thenReturn(emptyList())
      val optional = resetPasswordService.requestResetPassword("some.u’ser@SOMEwhere", "url")
      verify(notificationClient).sendEmail(
        eq("resetUnavailableEmailNotFoundTemplate"),
        eq("some.u'ser@somewhere"),
        check {
          assertThat(it).isEmpty()
        },
        isNull()
      )
      assertThat(optional).isEmpty
    }

    @Test
    fun requestResetPassword_multipleEmailAddresses() {
      val user = createSampleUser(
        username = "someuser",
        email = "email",
        person = Person("Bob", "Smith"),
        enabled = true
      )
      whenever(userRepository.findByEmail(any())).thenReturn(listOf(user, user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(user)

      val optional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url")
      verify(notificationClient).sendEmail(
        eq("resetTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(
            entry("firstName", "Bob"),
            entry("fullName", "Bob Smith"),
            entry("resetLink", optional.get())
          )
        },
        isNull()
      )
      assertThat(optional).isPresent
    }

    @Test
    fun requestResetPassword_multipleEmailAddresses_verifyToken() {
      val user = createSampleUser(
        username = "someuser",
        email = "email",
        person = Person("Bob", "Smith"),
        enabled = true
      )
      whenever(userRepository.findByEmail(any())).thenReturn(listOf(user, user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(user)
      val linkOptional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url")
      val userToken = user.tokens.stream().findFirst().orElseThrow()
      assertThat(linkOptional).get().isEqualTo(String.format("http://url-select?token=%s", userToken.token))
    }

    @Test
    fun requestResetPassword_multipleEmailAddresses_noneCanBeReset() {
      val user =
        createSampleUser(username = "someuser", email = "email", locked = true, person = Person("Bob", "Smith"))
      whenever(userRepository.findByEmail(any())).thenReturn(listOf(user, user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(user)
      val optional = resetPasswordService.requestResetPassword("someuser@somewhere", "http://url")
      verify(notificationClient).sendEmail(
        eq("resetUnavailableTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(entry("firstName", "Bob"), entry("fullName", "Bob Smith"))
        },
        isNull()
      )
      assertThat(optional).isEmpty
    }

    @Test
    fun requestResetPassword_byEmail() {
      val user = createSampleUser(username = "someuser", email = "email", locked = true, source = nomis)
      whenever(userRepository.findByEmail(anyString())).thenReturn(listOf(user))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBob)
      val optionalLink = resetPasswordService.requestResetPassword("user@where", "url")
      verify(notificationClient).sendEmail(
        eq("resetTemplate"),
        eq("email"),
        check {
          assertThat(it).containsOnly(
            entry("firstName", "Bob"),
            entry("fullName", "Bob Smith"),
            entry("resetLink", optionalLink.get())
          )
        },
        isNull()
      )
      assertThat(optionalLink).isPresent
    }

    @Test
    fun `Nomis User who has not logged into auth can reset password`() {
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
      val standardUser = buildStandardUser("user")
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(standardUser))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(standardUser)
      whenever(userService.getEmailAddressFromNomis(anyString())).thenReturn(Optional.of("Bob.smith@justice.gov.uk"))
      val optionalLink = resetPasswordService.requestResetPassword("user", "url")
      assertThat(optionalLink).isPresent
      verify(userRepository).save(
        check { user ->
          assertThat(user.username).isEqualTo("user")
          assertThat(user.email).isEqualTo("Bob.smith@justice.gov.uk")
          assertThat(user.verified).isTrue
          assertThat(user.source).isEqualTo(nomis)
        }
      )
    }

    @Test
    fun `Nomis User who has not logged reset password request no email`() {
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(buildStandardUser("user")))
      whenever(userService.getEmailAddressFromNomis(anyString())).thenReturn(Optional.empty())
      val optional = resetPasswordService.requestResetPassword("user", "url")
      assertThat(optional).isEmpty
    }

    @Test
    fun `Delius User who has not logged into DPS reset password request`() {
      val deliusUser = createDeliusUser()
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(deliusUser))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(deliusUser)
      val optionalLink = resetPasswordService.requestResetPassword("user", "url")
      assertThat(optionalLink).isPresent
    }

    @Test
    fun `Delius User not enabled who has not logged into DPS reset password request`() {
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
      val deliusUserNotEnabled = createDeliusUserNotEnabled()
      whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(deliusUserNotEnabled))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(deliusUserNotEnabled)
      val optional = resetPasswordService.requestResetPassword("user", "url")
      assertThat(optional).isEmpty
    }
  }

  private val staffUserAccountForBob: NomisUserPersonDetails
    get() {
      return nomisUserPersonDetails("OPEN")
    }
  private val staffUserAccountLockedForBob: NomisUserPersonDetails
    get() {
      return nomisUserPersonDetails("LOCKED")
    }
  private val staffUserAccountExpiredLockedForBob: NomisUserPersonDetails
    get() {
      return nomisUserPersonDetails("EXPIRED & LOCKED(TIMED)")
    }

  private fun nomisUserPersonDetails(accountStatus: String, staff: Staff = Staff(firstName = "bOb", status = "ACTIVE", lastName = "Smith", staffId = 5)): NomisUserPersonDetails =
    createSampleNomisUser(staff = staff, accountStatus = accountStatus)

  private val staffUserAccountForBobOptional: Optional<UserPersonDetails> = Optional.of(staffUserAccountForBob)
  private val staffUserAccountLockedForBobOptional: Optional<UserPersonDetails> =
    Optional.of(staffUserAccountLockedForBob)
  private val staffUserAccountExpiredLockedForBobOptional: Optional<UserPersonDetails> =
    Optional.of(staffUserAccountExpiredLockedForBob)

  @Nested
  inner class setPassword {
    @Test
    fun resetPassword() {
      val staffUserAccountForBob = staffUserAccountForBob
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBob)
      val user = createSampleUser(username = "user", person = Person("First", "Last"), source = nomis)
      val userToken = user.createToken(UserToken.TokenType.RESET)
      whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
      resetPasswordService.setPassword("bob", "pass")
      assertThat(user.tokens).isEmpty()
      verify(userRepository).save(user)
      verify(delegatingUserService).changePasswordWithUnlock(staffUserAccountForBob, "pass")
      verify(notificationClient).sendEmail(
        "reset-confirm",
        null,
        mapOf("firstName" to "First", "fullName" to "First Last", "username" to "user"),
        null
      )
    }

    @Test
    fun resetPassword_authUser() {
      val user = createSampleUser(
        username = "user",
        person = Person("First", "Last"),
        enabled = true,
        source = auth,
        locked = true
      )
      val userToken = user.createToken(UserToken.TokenType.RESET)
      whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
      whenever(userService.findEnabledMasterUserPersonDetails("user")).thenReturn(user)
      resetPasswordService.setPassword("bob", "pass")
      assertThat(user.tokens).isEmpty()
      verify(userRepository).save(user)
      verify(delegatingUserService).changePasswordWithUnlock(any(), anyString())
      verify(notificationClient).sendEmail(
        "reset-confirm",
        null,
        mapOf("firstName" to "First", "fullName" to "First Last", "username" to "user"),
        null
      )
    }

    @Test
    fun resetPassword_deliusUser() {
      val user =
        createSampleUser(
          username = "user",
          person = Person("First", "Last"),
          enabled = true,
          source = delius,
          locked = true
        )
      val userToken = user.createToken(UserToken.TokenType.RESET)
      val deliusUserPersonDetails =
        DeliusUserPersonDetails("user", "12345", "Delius", "Smith", "a@b.com", true, false, setOf())
      whenever(userService.findEnabledMasterUserPersonDetails("user")).thenReturn(deliusUserPersonDetails)
      whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
      resetPasswordService.setPassword("bob", "pass")
      assertThat(user.tokens).isEmpty()
      verify(userRepository).save(user)
      verify(delegatingUserService).changePasswordWithUnlock(any(), anyString())
      verify(notificationClient).sendEmail(
        "reset-confirm",
        null,
        mapOf("firstName" to "First", "fullName" to "First Last", "username" to "user"),
        null
      )
    }

    @Test
    fun resetPasswordLockedAccount() {
      val staffUserAccount =
        nomisUserPersonDetails("OPEN", Staff(firstName = "bOb", status = "inactive", lastName = "Smith", staffId = 5))
      whenever(userService.findEnabledMasterUserPersonDetails(anyString())).thenReturn(staffUserAccount)
      val user = createSampleUser(username = "user", source = nomis, locked = true)
      val userToken = user.createToken(UserToken.TokenType.RESET)
      whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
      assertThatThrownBy { resetPasswordService.setPassword("bob", "pass") }.isInstanceOf(LockedException::class.java)
    }

    @Test
    fun resetPasswordLockedAccount_authUser() {
      val user = createSampleUser(username = "user")
      val userToken = user.createToken(UserToken.TokenType.RESET)
      whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
      whenever(userService.findEnabledMasterUserPersonDetails("user")).thenReturn(user)
      assertThatThrownBy { resetPasswordService.setPassword("bob", "pass") }.isInstanceOf(LockedException::class.java)
    }

    @Test
    fun `set password no enabled account`() {
      val user = createSampleUser(username = "user")
      val userToken = user.createToken(UserToken.TokenType.RESET)
      whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
      assertThatThrownBy { resetPasswordService.setPassword("bob", "pass") }.isInstanceOf(ResetPasswordException::class.java)
    }
  }

  @Test
  fun moveTokenToAccount_missingUsername() {
    assertThatThrownBy {
      resetPasswordService.moveTokenToAccount(
        "token",
        "  "
      )
    }.hasMessageContaining("failed with reason: missing")
  }

  @Test
  fun moveTokenToAccount_usernameNotFound() {
    assertThatThrownBy {
      resetPasswordService.moveTokenToAccount(
        "token",
        "noone"
      )
    }.hasMessageContaining("failed with reason: notfound")
  }

  @Test
  fun moveTokenToAccount_differentEmail() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(
      Optional.of(
        createSampleUser(username = "user", email = "email", verified = true, locked = true)
      )
    )
    val builtUser = createSampleUser(username = "other", email = "emailother", verified = true)
    whenever(userTokenRepository.findById("token")).thenReturn(Optional.of(builtUser.createToken(UserToken.TokenType.RESET)))
    assertThatThrownBy {
      resetPasswordService.moveTokenToAccount(
        "token",
        "noone"
      )
    }.hasMessageContaining("failed with reason: email")
  }

  @Test
  fun moveTokenToAccount_disabled() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(
      Optional.of(createSampleUser(username = "user", email = "email", verified = true))
    )
    val builtUser = createSampleUser(username = "other", email = "email", verified = true)
    whenever(userTokenRepository.findById("token")).thenReturn(Optional.of(builtUser.createToken(UserToken.TokenType.RESET)))
    assertThatThrownBy { resetPasswordService.moveTokenToAccount("token", "noone") }.extracting("reason")
      .isEqualTo("locked")
  }

  @Test
  fun moveTokenToAccount_sameUserAccount() {
    val user = createSampleUser(username = "USER", email = "email", verified = true, enabled = true)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(userTokenRepository.findById("token")).thenReturn(Optional.of(user.createToken(UserToken.TokenType.RESET)))
    val newToken = resetPasswordService.moveTokenToAccount("token", "USER")
    assertThat(newToken).isEqualTo("token")
  }

  @Test
  fun moveTokenToAccount_differentAccount() {
    val user = createSampleUser(username = "USER", email = "email", verified = true, enabled = true)
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val builtUser = createSampleUser(username = "other", email = "email", verified = true)
    val userToken = builtUser.createToken(UserToken.TokenType.RESET)
    whenever(userTokenRepository.findById("token")).thenReturn(Optional.of(userToken))
    val newToken = resetPasswordService.moveTokenToAccount("token", "USER")
    assertThat(newToken).hasSize(36)
    verify(userTokenRepository).delete(userToken)
    assertThat(user.tokens).extracting<String> { obj: UserToken -> obj.token }.containsExactly(newToken)
  }

  private fun createDeliusUser() =
    DeliusUserPersonDetails(
      username = "user",
      userId = "12345",
      firstName = "F",
      surname = "L",
      email = "a@b.com",
      enabled = true
    )

  private fun createDeliusUserNotEnabled() =
    DeliusUserPersonDetails(
      username = "user",
      userId = "12345",
      firstName = "F",
      surname = "L",
      email = "a@b.com",
      enabled = false
    )

  private fun buildStandardUser(username: String): NomisUserPersonDetails {
    val staff = buildStaff()
    val roles = listOf(
      UserCaseloadRole(
        id = UserCaseloadRoleIdentity(caseload = "NWEB", roleId = ROLE_ID, username = username),
        role = Role(code = "ROLE1", id = ROLE_ID),
      )
    )
    return NomisUserPersonDetails(
      username = username,
      password = "pass",
      type = "GENERAL",
      staff = staff,
      roles = roles,
      accountDetail = buildAccountDetail(username, AccountStatus.OPEN),
      activeCaseLoadId = null
    )
  }

  private fun buildStaff(): Staff = Staff(firstName = "Bob", status = "ACTIVE", lastName = "Smith", staffId = 1)

  private fun buildAccountDetail(username: String, status: AccountStatus): AccountDetail = AccountDetail(
    username = username,
    accountStatus = status.desc,
    profile = "TAG_GENERAL"
  )

  companion object {
    const val ROLE_ID = 1L
  }
}

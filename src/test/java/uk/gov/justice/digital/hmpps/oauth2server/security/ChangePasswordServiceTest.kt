package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.security.authentication.LockedException
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetailsHelper.Companion.createSampleNomisUser
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService
import java.util.Optional

class ChangePasswordServiceTest {
  private val userRepository: UserRepository = mock()
  private val userTokenRepository: UserTokenRepository = mock()
  private val userService: UserService = mock()
  private val delegatingUserService: DelegatingUserService = mock()
  private var changePasswordService =
    ChangePasswordService(userTokenRepository, userRepository, userService, delegatingUserService)

  @Test
  fun setPassword_AlterUser() {
    val staffUserAccountForBob = staffUserAccountForBob
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(staffUserAccountForBob))
    val user = User.builder().username("user").locked(true).build()
    val userToken = user.createToken(UserToken.TokenType.RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    changePasswordService.setPassword("bob", "pass")
    verify(delegatingUserService).changePassword(staffUserAccountForBob, "pass")
  }

  @Test
  fun setPassword_AuthUser() {
    val user = User.builder().username("user").email("email").enabled(true).source(AuthSource.auth).build()
    val userToken = user.createToken(UserToken.TokenType.RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    changePasswordService.setPassword("bob", "pass")
    verify(delegatingUserService).changePassword(user, "pass")
  }

  @Test
  fun setPassword_SaveAndDelete() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBobOptional)
    val user = User.builder().username("user").locked(true).build()
    val userToken = user.createToken(UserToken.TokenType.RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    changePasswordService.setPassword("bob", "pass")

    // need to ensure that the token has been removed as don't want them to be able to change password multiple times
    assertThat(user.tokens).isEmpty()
    verify(userRepository).save(user)
  }

  @Test
  fun setPassword_AuthUserPasswordSet() {
    val user = User.builder().username("user").enabled(true).source(AuthSource.auth).build()
    val userToken = user.createToken(UserToken.TokenType.RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    changePasswordService.setPassword("bob", "pass")
    verify(delegatingUserService).changePassword(user, "pass")
  }

  @Test
  fun setPassword_LockedAccount() {
    val staffUserAccount = staffUserAccountLockedForBobOptional
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(staffUserAccount)
    val user = User.of("user")
    val userToken = user.createToken(UserToken.TokenType.RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThatThrownBy { changePasswordService.setPassword("bob", "pass") }.isInstanceOf(LockedException::class.java)
  }

  @Test
  fun setPassword_DisabledAccount() {
    val optionalUser = buildAuthUser()
    optionalUser.map { User::class.java.cast(it) }.get().isEnabled = false
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(optionalUser)
    val user = User.of("user")
    val userToken = user.createToken(UserToken.TokenType.RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThatThrownBy { changePasswordService.setPassword("bob", "pass") }.isInstanceOf(LockedException::class.java)
  }

  @Test
  fun setPassword_LockedAuthAccount() {
    val user = User.builder().username("user").locked(true).enabled(true).source(AuthSource.auth).build()
    user.isEnabled = true
    user.source = AuthSource.auth
    val userToken = user.createToken(UserToken.TokenType.RESET)
    whenever(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken))
    assertThatThrownBy { changePasswordService.setPassword("bob", "pass") }.isInstanceOf(LockedException::class.java)
  }

  private fun buildAuthUser(): Optional<UserPersonDetails> {
    val user = User.builder().username("user").email("email").verified(true)
      .person(Person("first", "last")).enabled(true).build()
    return Optional.of(user)
  }

  private val staffUserAccountForBob: UserPersonDetails
    get() {
      return nomisUserPersonDetails("OPEN")
    }

  private val staffUserAccountLockedForBob: UserPersonDetails
    get() {
      return nomisUserPersonDetails("LOCKED")
    }
  private fun nomisUserPersonDetails(accountStatus: String): NomisUserPersonDetails =
    createSampleNomisUser(staff = Staff(firstName = "bOb", status = "ACTIVE", lastName = "bloggs", staffId = 5), accountStatus = accountStatus)

  private val staffUserAccountForBobOptional = Optional.of(staffUserAccountForBob)
  private val staffUserAccountLockedForBobOptional = Optional.of(staffUserAccountLockedForBob)
}

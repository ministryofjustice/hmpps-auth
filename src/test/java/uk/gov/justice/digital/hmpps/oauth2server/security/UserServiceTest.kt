package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import java.util.*

class UserServiceTest {
  private val nomisUserService: NomisUserService = mock()
  private val authUserService: AuthUserService = mock()
  private val deliusUserService: DeliusUserService = mock()
  private val userRepository: UserRepository = mock()
  private val userService = UserService(nomisUserService, authUserService, deliusUserService, userRepository)

  @Test
  fun `findMasterUserPersonDetails auth user`() {
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(createUser())
    val user = userService.findMasterUserPersonDetails("   bob   ")
    assertThat(user).isPresent.get().extracting { it.username }.isEqualTo("someuser")
  }

  @Test
  fun `findMasterUserPersonDetails nomis user`() {
    whenever(nomisUserService.getNomisUserByUsername(anyString())).thenReturn(staffUserAccountForBob)
    val user = userService.findMasterUserPersonDetails("bob")
    assertThat(user).isPresent.get().extracting { it.username }.isEqualTo("nomisuser")
  }

  @Test
  fun `findMasterUserPersonDetails delius user`() {
    whenever(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(deliusUserAccountForBob)
    val user = userService.findMasterUserPersonDetails("bob")
    assertThat(user).isPresent.get().extracting { it.username }.isEqualTo("deliusUser")
  }

  @Test
  fun findUser() {
    val user = createUser()
    whenever(userRepository.findByUsername(anyString())).thenReturn(user)
    val found = userService.findUser("bob")
    assertThat(found).isSameAs(user)
    verify(userRepository).findByUsername("BOB")
  }

  @Test
  fun `getOrCreateUser user exists already`() {
    val user = User.of("joe")
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val newUser = userService.getOrCreateUser("bob")
    assertThat(newUser).isSameAs(user)
  }

  @Test
  fun `getOrCreateUser no user already`() {
    val user = User.of("joe")
    whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(userRepository.save<User>(any())).thenReturn(user)
    val newUser = userService.getOrCreateUser("bob")
    assertThat(newUser).isSameAs(user)
  }

  @Test
  fun `hasVerifiedEmail success`() {
    val user = User.builder().username("joe").email("someemail").verified(true).build()
    assertThat(userService.hasVerifiedEmail(user)).isTrue()
  }

  @Test
  fun `hasVerifiedEmail no email`() {
    val user = User.builder().username("joe").verified(true).build()
    assertThat(userService.hasVerifiedEmail(user)).isFalse()
  }

  @Test
  fun `hasVerifiedEmail not verified`() {
    val user = User.builder().username("joe").email("someemail").build()
    assertThat(userService.hasVerifiedEmail(user)).isFalse()
  }

  @Test
  fun `isSameAsCurrentVerifiedMobile not verified`() {
    val user = User.builder().mobileVerified(false).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "")
    assertThat(returnValue).isFalse()
  }

  @Test
  fun `isSameAsCurrentVerifiedMobile new different mobile number`() {
    val user = User.builder().mobile("07700900001").mobileVerified(true).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "07700900000")
    assertThat(returnValue).isFalse()
  }

  @Test
  fun `isSameAsCurrentVerifiedMobile new different mobile number with whitespace`() {
    val user = User.builder().mobile("07700900001").mobileVerified(true).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "0770 090 0000")
    assertThat(returnValue).isFalse()
  }

  @Test
  fun `isSameAsCurrentVerifiedMobile same mobile number`() {
    val user = User.builder().mobile("07700900000").mobileVerified(true).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "07700900000")
    assertThat(returnValue).isTrue()
  }

  @Test
  fun `isSameAsCurrentVerifiedMobile same mobile number with whitespace`() {
    val user = User.builder().mobile("07700900000").mobileVerified(true).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "0770 090 0000")
    assertThat(returnValue).isTrue()
  }

  private fun createUser() = Optional.of(User.of("someuser"))

  private val staffUserAccountForBob: Optional<NomisUserPersonDetails>
    get() {
      val staffUserAccount = NomisUserPersonDetails()
      staffUserAccount.username = "nomisuser"
      val staff = Staff()
      staff.firstName = "bOb"
      staff.status = "ACTIVE"
      staffUserAccount.staff = staff
      val detail = AccountDetail("user", "OPEN", "profile", null)
      staffUserAccount.accountDetail = detail
      return Optional.of(staffUserAccount)
    }

  private val deliusUserAccountForBob =
      Optional.of(DeliusUserPersonDetails("deliusUser", "12345", "Delius", "Smith", "a@b.com", true, false, setOf()))
}

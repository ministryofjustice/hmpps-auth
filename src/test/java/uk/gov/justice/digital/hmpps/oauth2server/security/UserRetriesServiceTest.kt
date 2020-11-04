package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserRetries
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService
import java.time.LocalDateTime
import java.util.Optional

class UserRetriesServiceTest {
  private val userRetriesRepository: UserRetriesRepository = mock()
  private val userRepository: UserRepository = mock()
  private val delegatingUserService: DelegatingUserService = mock()
  private val userService: UserService = mock()
  private val service = UserRetriesService(userRetriesRepository, userRepository, delegatingUserService, userService, 3)

  @Nested
  inner class resetRetriesAndRecordLogin {
    @Test
    fun resetRetriesAndRecordLogin() {
      whenever(userService.getEmailAddressFromNomis(anyString())).thenReturn(Optional.of("bob@bob.justice.gov.uk"))
      service.resetRetriesAndRecordLogin(userPersonDetailsForBob)
      verify(userRetriesRepository).save<UserRetries>(
        check {
          assertThat(it).isEqualTo(UserRetries("bob", 0))
        }
      )
    }

    @Test
    fun resetRetriesAndRecordLogin_RecordLastLogginIn() {
      val user = User.builder().username("joe").lastLoggedIn(LocalDateTime.now().minusDays(1)).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      service.resetRetriesAndRecordLogin(userPersonDetailsForBob)
      assertThat(user.lastLoggedIn).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now())
    }

    @Test
    fun `resetRetriesAndRecordLogin save delius email address existing user`() {
      val user = User.builder().username("joe").lastLoggedIn(LocalDateTime.now().minusDays(1)).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      service.resetRetriesAndRecordLogin(
        DeliusUserPersonDetails(
          "deliusUser",
          "12345",
          "Delius",
          "Smith",
          "newemail@bob.com",
          true,
          false,
          emptySet()
        )
      )
      assertThat(user.email).isEqualTo("newemail@bob.com")
      assertThat(user.isVerified).isTrue()
    }

    @Test
    fun `resetRetriesAndRecordLogin save delius email address new user`() {
      service.resetRetriesAndRecordLogin(
        DeliusUserPersonDetails(
          "deliusUser",
          "12345",
          "Delius",
          "Smith",
          "newemail@bob.com",
          true,
          false,
          emptySet()
        )
      )
      verify(userRepository).save(
        check { user ->
          assertThat(user.email).isEqualTo("newemail@bob.com")
          assertThat(user.isVerified).isTrue()
        }
      )
    }

    @Test
    fun resetRetriesAndRecordLogin_SaveNewUserWithNomisEmailVerified() {
      whenever(userService.getEmailAddressFromNomis(anyString())).thenReturn(Optional.of("bob@bob.justice.gov.uk"))
      service.resetRetriesAndRecordLogin(userPersonDetailsForBob)
      verify(userRepository).save<User>(
        check {
          assertThat(it.username).isEqualTo("bob")
          assertThat(it.email).isEqualTo("bob@bob.justice.gov.uk")
          assertThat(it.isVerified).isTrue()
          assertThat(it.lastLoggedIn).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now())
        }
      )
    }

    @Test
    fun resetRetriesAndRecordLogin_SaveNewNomisUserNoEmailAsNotJusticeEmail() {
      whenever(userService.getEmailAddressFromNomis(anyString())).thenReturn(Optional.empty())
      service.resetRetriesAndRecordLogin(userPersonDetailsForBob)
      verify(userRepository).save<User>(
        check {
          assertThat(it.username).isEqualTo("bob")
          assertThat(it.isVerified).isFalse()
          assertThat(it.lastLoggedIn).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now())
        }
      )
    }
  }

  @Nested
  inner class incrementRetriesAndLockAccountIfNecessary {
    @Test
    fun incrementRetriesAndLockAccountIfNecessary_retriesTo0() {
      service.incrementRetriesAndLockAccountIfNecessary(userPersonDetailsForBob)
      verify(userRetriesRepository).save<UserRetries>(
        check {
          assertThat(it).isEqualTo(UserRetries("bob", 0))
        }
      )
    }

    @Test
    fun incrementRetriesAndLockAccountIfNecessary_moreAttemptsAllowed() {
      whenever(userRetriesRepository.findById(anyString())).thenReturn(Optional.of(UserRetries("bob", 1)))
      val userPersonDetailsForBob = userPersonDetailsForBob
      service.incrementRetriesAndLockAccountIfNecessary(userPersonDetailsForBob)
      verify(delegatingUserService, never()).lockAccount(any())
    }

    @Test
    fun incrementRetriesAndLockAccountIfNecessary_lockAccount() {
      whenever(userRetriesRepository.findById(anyString())).thenReturn(Optional.of(UserRetries("bob", 2)))
      val userPersonDetailsForBob = userPersonDetailsForBob
      service.incrementRetriesAndLockAccountIfNecessary(userPersonDetailsForBob)
      verify(delegatingUserService).lockAccount(userPersonDetailsForBob)
    }

    @Test
    fun incrementRetriesAndLockAccountIfNecessary_NoExistingRow() {
      whenever(userRetriesRepository.findById(anyString())).thenReturn(Optional.empty())
      assertThat(service.incrementRetriesAndLockAccountIfNecessary(userPersonDetailsForBob)).isEqualTo(false)
      verify(userRetriesRepository).save<UserRetries>(
        check {
          assertThat(it).isEqualTo(UserRetries("bob", 11))
        }
      )
    }

    @Test
    fun incrementRetriesAndLockAccountIfNecessary_ExistingRow() {
      whenever(userRetriesRepository.findById(anyString())).thenReturn(Optional.of(UserRetries("bob", 5)))
      assertThat(service.incrementRetriesAndLockAccountIfNecessary(userPersonDetailsForBob)).isEqualTo(true)
      verify(userRetriesRepository).save<UserRetries>(
        check {
          assertThat(it).isEqualTo(UserRetries("bob", 6))
        }
      )
    }
  }

  @Nested
  inner class resetRetries {
    @Test
    fun resetRetries() {
      service.resetRetries("bob")
      verify(userRetriesRepository).save<UserRetries>(
        check {
          assertThat(it).isEqualTo(UserRetries("bob", 0))
        }
      )
    }
  }

  private val userPersonDetailsForBob: UserPersonDetails
    get() {
      val staffUserAccount = NomisUserPersonDetails()
      staffUserAccount.staff = Staff(firstName = "bOb", status = "ACTIVE", lastName = "bloggs", staffId = 5)
      val detail = AccountDetail("user", "OPEN", "profile", null)
      staffUserAccount.accountDetail = detail
      staffUserAccount.username = "bob"
      return staffUserAccount
    }
}

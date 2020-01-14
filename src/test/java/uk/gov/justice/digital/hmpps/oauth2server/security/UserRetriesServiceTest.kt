package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
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
import java.util.*

class UserRetriesServiceTest {
  private val userRetriesRepository: UserRetriesRepository = mock()
  private val userRepository: UserRepository = mock()
  private val delegatingUserService: DelegatingUserService = mock()
  private lateinit var service: UserRetriesService

  @Before
  fun setUp() {
    service = UserRetriesService(userRetriesRepository, userRepository, delegatingUserService, 3)
  }

  @Test
  fun resetRetriesAndRecordLogin() {
    service.resetRetriesAndRecordLogin(userPersonDetailsForBob)
    verify(userRetriesRepository).save<UserRetries>(check {
      assertThat(it).isEqualTo(UserRetries("bob", 0))
    })
  }

  @Test
  fun resetRetriesAndRecordLogin_RecordLastLogginIn() {
    val user = User.builder().username("joe").lastLoggedIn(LocalDateTime.now().minusDays(1)).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    service.resetRetriesAndRecordLogin(userPersonDetailsForBob)
    assertThat(user.lastLoggedIn).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now())
  }

  @Test
  fun resetRetriesAndRecordLogin_SaveDeliusEmailAddress() {
    val user = User.builder().username("joe").lastLoggedIn(LocalDateTime.now().minusDays(1)).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    service.resetRetriesAndRecordLogin(DeliusUserPersonDetails("deliusUser", "12345", "Delius", "Smith", "newemail@bob.com", true, false, emptySet()))
    assertThat(user.email).isEqualTo("newemail@bob.com")
    assertThat(user.isVerified).isTrue()
  }

  @Test
  fun resetRetriesAndRecordLogin_SaveNewUser() {
    service.resetRetriesAndRecordLogin(userPersonDetailsForBob)
    verify(userRepository).save<User>(check {
      assertThat(it.username).isEqualTo("bob")
      assertThat(it.lastLoggedIn).isBetween(LocalDateTime.now().plusMinutes(-1), LocalDateTime.now())
    })
  }

  @Test
  fun incrementRetriesAndLockAccountIfNecessary_retriesTo0() {
    service.incrementRetriesAndLockAccountIfNecessary(userPersonDetailsForBob)
    verify(userRetriesRepository).save<UserRetries>(check {
      assertThat(it).isEqualTo(UserRetries("bob", 0))
    })
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
    verify(userRetriesRepository).save<UserRetries>(check {
      assertThat(it).isEqualTo(UserRetries("bob", 11))
    })
  }

  @Test
  fun incrementRetriesAndLockAccountIfNecessary_ExistingRow() {
    whenever(userRetriesRepository.findById(anyString())).thenReturn(Optional.of(UserRetries("bob", 5)))
    assertThat(service.incrementRetriesAndLockAccountIfNecessary(userPersonDetailsForBob)).isEqualTo(true)
    verify(userRetriesRepository).save<UserRetries>(check {
      assertThat(it).isEqualTo(UserRetries("bob", 6))
    })
  }

  @Test
  fun resetRetries() {
    service.resetRetries("bob")
    verify(userRetriesRepository).save<UserRetries>(check {
      assertThat(it).isEqualTo(UserRetries("bob", 0))
    })
  }

  private val userPersonDetailsForBob: UserPersonDetails
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
}

package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.service.notify.NotificationClientApi
import java.util.Map.entry
import java.util.Optional
import javax.persistence.EntityNotFoundException

class InitialPasswordServiceTest {
  private val userRepository: UserRepository = mock()
  private val oauthServiceRepository: OauthServiceRepository = mock()
  private val userService: UserService = mock()
  private val notificationClient: NotificationClientApi = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val initialPasswordService = InitialPasswordService(
    userRepository,
    oauthServiceRepository,
    userService,
    notificationClient,
    "resendTemplate",
    telemetryClient
  )

  @Test
  fun `resend Initial Password Link`() {
    val user = User.builder().username("someuser").person(Person("Bob", "Smith")).email("email").build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBobOptional)
    val service = Service("serviceCode", "service", "service", "ANY_ROLES", "ANY_URL", true, "supportLink")
    whenever(oauthServiceRepository.findById(anyString())).thenReturn(Optional.of(service))
    val link = initialPasswordService.resendInitialPasswordLink("user", "url-expired")
    verify(notificationClient).sendEmail(
      eq("resendTemplate"),
      eq("email"),
      check {
        assertThat(it).containsOnly(
          entry("firstName", "Bob"),
          entry("fullName", "Bob Smith"),
          entry("resetLink", link),
          entry("supportLink", "supportLink")
        )
      },
      isNull()
    )
    assertThat(link).isNotEmpty()
  }

  @Test
  fun `resend Initial Password Link check telemetry`() {
    val user = User.builder().username("someuser").person(Person("Bob", "Smith")).email("email").build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(staffUserAccountForBobOptional)
    val service = Service("serviceCode", "service", "service", "ANY_ROLES", "ANY_URL", true, "supportLink")
    whenever(oauthServiceRepository.findById(anyString())).thenReturn(Optional.of(service))
    initialPasswordService.resendInitialPasswordLink("user", "url-expired")
    verify(telemetryClient).trackEvent("reissueInitialPasswordLink", mapOf("username" to "someuser"), null)
  }

  @Test
  fun `resend Initial Password Link User not found`() {
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy { initialPasswordService.resendInitialPasswordLink("user", "url-expired") }.isInstanceOf(
      EntityNotFoundException::class.java
    )
  }

  @Test
  fun `resend Initial Password Link Master User not found`() {
    val user =
      User.builder().username("someuser").person(Person("Bob", "Smith")).email("email").source(AuthSource.nomis).build()
    whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy { initialPasswordService.resendInitialPasswordLink("user", "url-expired") }.isInstanceOf(
      EntityNotFoundException::class.java
    )
  }

  private val staffUserAccountForBob: UserPersonDetails
    get() {
      val staffUserAccount = NomisUserPersonDetails()
      staffUserAccount.staff = Staff(firstName = "bOb", status = "ACTIVE", lastName = "Smith", staffId = 5)
      val detail = AccountDetail("user", "OPEN", "profile", null)
      staffUserAccount.accountDetail = detail
      return staffUserAccount
    }

  private val staffUserAccountForBobOptional: Optional<UserPersonDetails> = Optional.of(staffUserAccountForBob)
}

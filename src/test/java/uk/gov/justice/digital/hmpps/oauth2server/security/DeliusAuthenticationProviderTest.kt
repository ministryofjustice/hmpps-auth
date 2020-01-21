package uk.gov.justice.digital.hmpps.oauth2server.security

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import java.util.*

class DeliusAuthenticationProviderTest {
  private val deliusUserService: DeliusUserService = mock()
  private val userService: UserService = mock()
  private val userRetriesService: UserRetriesService = mock()
  private val mfaService: MfaService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val provider = DeliusAuthenticationProvider(deliusUserService, DeliusUserDetailsService(deliusUserService, userService), userRetriesService, mfaService, userService, telemetryClient)

  @Test
  fun authenticate_Success() {
    whenever(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(
        Optional.of(DeliusUserPersonDetails("bob", "12345", "Delius", "Smith", "a@b.com", true, false, emptySet())))
    whenever(deliusUserService.authenticateUser(anyString(), anyString())).thenReturn(true)
    val auth = provider.authenticate(UsernamePasswordAuthenticationToken("DELIUS_USER", "password"))
    assertThat(auth).isNotNull()
  }

  @Test
  fun authenticate_SuccessWithAuthorities() {
    whenever(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(Optional.of(
        DeliusUserPersonDetails("bob", "12345", "Delius", "Smith", "a@b.com", true, false, listOf(SimpleGrantedAuthority("ROLE_BOB")))))
    whenever(deliusUserService.authenticateUser(anyString(), anyString())).thenReturn(true)
    val auth = provider.authenticate(UsernamePasswordAuthenticationToken("ITAG_USER_ADM", "password123456"))
    assertThat(auth).isNotNull()
    assertThat(auth.authorities).extracting<String, RuntimeException> { obj: GrantedAuthority -> obj.authority }.containsOnly("ROLE_PROBATION", "ROLE_BOB")
  }

  @Test
  fun authenticate_NullUsername() {
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken(null, "password")) }.isInstanceOf(MissingCredentialsException::class.java)
  }

  @Test
  fun authenticate_MissingUsername() {
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken("      ", "password")) }.isInstanceOf(MissingCredentialsException::class.java)
  }

  @Test
  fun authenticate_MissingPassword() {
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken("ITAG_USER", "   ")) }.isInstanceOf(MissingCredentialsException::class.java)
  }

  @Test
  fun authenticate_LockAfterThreeFailures() {
    val deliusUserPersonDetails = DeliusUserPersonDetails("bob", "12345", "Delius", "Smith", "a@b.com", true, false, emptySet())
    whenever(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(Optional.of(deliusUserPersonDetails))
    whenever(userRetriesService.incrementRetriesAndLockAccountIfNecessary(any())).thenReturn(true)
    assertThatThrownBy { provider.authenticate(UsernamePasswordAuthenticationToken("CA_USER", "wrong")) }.isInstanceOf(LockedException::class.java)
  }

  @Test
  fun authenticate_ResetAfterSuccess() {
    val deliusUser = DeliusUserPersonDetails("bob", "12345", "Delius", "Smith", "a@b.com", true, false, emptySet())
    whenever(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(Optional.of(deliusUser))
    whenever(deliusUserService.authenticateUser(anyString(), anyString())).thenReturn(true)
    provider.authenticate(UsernamePasswordAuthenticationToken("DELIUS_USER", "password"))
    verify(userRetriesService).resetRetriesAndRecordLogin(deliusUser)
  }
}

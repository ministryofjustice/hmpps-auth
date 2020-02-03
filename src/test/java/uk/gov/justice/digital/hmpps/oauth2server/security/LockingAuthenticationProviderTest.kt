package uk.gov.justice.digital.hmpps.oauth2server.security

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaRequiredException
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService

class LockingAuthenticationProviderTest {
  private val userRetriesService: UserRetriesService = mock()
  private val userDetailsService: AuthUserDetailsService = mock()
  private val userService: UserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val mfaService: MfaService = mock()

  private val lockingAuthenticationProvider = AuthAuthenticationProvider(userDetailsService, userRetriesService, mfaService, userService, telemetryClient)

  @Test
  fun `authenticate nomisUser`() { // test that oracle passwords are authenticated okay
    val password = "S:39BA463D55E5C8936A6798CC37B1347BA8BEC37B6407397EB769BC356F0C"
    val userDetails = UserDetailsImpl("user", "name", password,
        true, true, true, true, emptyList(), "none", null)
    whenever(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails)
    lockingAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken("user", "somepass1"))
  }

  @Test
  fun `authenticate authUser`() {
    val password = "{bcrypt}" + BCryptPasswordEncoder().encode("some_pass")
    val userDetails = UserDetailsImpl("user", "name", password,
        true, true, true, true, emptyList(), "none", null)
    whenever(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails)
    lockingAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken("user", "some_pass"))
  }

  @Test
  fun `authenticate authUser needs MFA`() {
    whenever(mfaService.needsMfa(any())).thenReturn(true)
    whenever(userService.hasVerifiedEmail(any())).thenReturn(true)

    val password = "{bcrypt}" + BCryptPasswordEncoder().encode("some_pass")
    val userDetails = UserDetailsImpl("user", "name", password,
        true, true, true, true, emptyList(), "none", null)
    whenever(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails)

    assertThatThrownBy {
      lockingAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken("user", "some_pass"))
    }.isInstanceOf(MfaRequiredException::class.java)
  }

  @Test
  fun `authenticate authUser MFA unavailable`() {
    whenever(mfaService.needsMfa(any())).thenReturn(true)

    val password = "{bcrypt}" + BCryptPasswordEncoder().encode("some_pass")
    val userDetails = UserDetailsImpl("user", "name", password,
        true, true, true, true, emptyList(), "none", null)
    whenever(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails)

    assertThatThrownBy {
      lockingAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken("user", "some_pass"))
    }.isInstanceOf(MfaUnavailableException::class.java)
  }
}

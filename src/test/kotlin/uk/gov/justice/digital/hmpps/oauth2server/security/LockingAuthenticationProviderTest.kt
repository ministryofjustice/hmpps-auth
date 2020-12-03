package uk.gov.justice.digital.hmpps.oauth2server.security

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaRequiredException
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService

class LockingAuthenticationProviderTest {
  private val userRetriesService: UserRetriesService = mock()
  private val userDetailsService: AuthUserDetailsService = mock()
  private val userService: UserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val mfaService: MfaService = mock()

  private val lockingAuthenticationProvider =
    AuthAuthenticationProvider(userDetailsService, userRetriesService, mfaService, userService, telemetryClient)

  @Test
  fun `authenticate nomisUser`() { // test that oracle passwords are authenticated okay
    setupLoadUser("S:39BA463D55E5C8936A6798CC37B1347BA8BEC37B6407397EB769BC356F0C")
    lockingAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken("user", "somepass1"))
  }

  @Test
  fun `authenticate authUser`() {
    setupLoadUser("{bcrypt}${BCryptPasswordEncoder().encode("some_pass")}")
    lockingAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken("user", "some_pass"))
  }

  @Test
  fun `authenticate authUser needs MFA`() {
    whenever(mfaService.needsMfa(any())).thenReturn(true)
    whenever(userService.hasVerifiedMfaMethod(any())).thenReturn(true)

    setupLoadUser("{bcrypt}${BCryptPasswordEncoder().encode("some_pass")}")

    assertThatThrownBy {
      lockingAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken("user", "some_pass"))
    }.isInstanceOf(MfaRequiredException::class.java)
  }

  @Test
  fun `authenticate authUser MFA unavailable`() {
    whenever(mfaService.needsMfa(any())).thenReturn(true)

    setupLoadUser("{bcrypt}${BCryptPasswordEncoder().encode("some_pass")}")

    assertThatThrownBy {
      lockingAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken("user", "some_pass"))
    }.isInstanceOf(MfaUnavailableException::class.java)
  }

  private fun setupLoadUser(password: String) {
    val userDetails = UserDetailsImpl("user", "name", emptyList(), "none", "user", "jwtId")
    ReflectionTestUtils.setField(userDetails, "password", password)
    whenever(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails)
  }
}

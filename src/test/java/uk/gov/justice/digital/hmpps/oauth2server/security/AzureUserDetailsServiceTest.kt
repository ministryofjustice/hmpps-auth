package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.azure.AzureUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.azure.service.AzureUserService
import java.util.ArrayList
import java.util.Optional

internal class AzureUserDetailsServiceTest {
  private val mockAzureUserService: AzureUserService = mock()

  @Test
  fun `loadUserByUsername returns user from AzureUserService unmodified`() {
    val azureUserDetailsService = AzureUserDetailsService(mockAzureUserService)

    val user = Optional.of(getAzureUserPersonDetails())

    whenever(
      mockAzureUserService
        .getAzureUserByUsername("D6165AD0-AED3-4146-9EF7-222876B57549")
    ).thenReturn(user)

    val returnedUser = azureUserDetailsService
      .loadUserByUsername("D6165AD0-AED3-4146-9EF7-222876B57549")

    assertThat(returnedUser).isSameAs(user.get())
  }

  @Test
  fun `loadUserByUsername throws exception if user not found`() {
    val azureUserDetailsService = AzureUserDetailsService(mockAzureUserService)

    whenever(
      mockAzureUserService
        .getAzureUserByUsername(any())
    ).thenReturn(Optional.empty())

    Assertions.assertThatThrownBy {
      azureUserDetailsService
        .loadUserByUsername("D6165AD0-AED3-4146-9EF7-222876B57549")
    }
      .isInstanceOf(UsernameNotFoundException::class.java)
  }

  @Test
  fun `loadUserDetails gets user by username in token principal`() {
    val azureUserDetailsService = AzureUserDetailsService(mockAzureUserService)

    val user = Optional.of(getAzureUserPersonDetails())

    whenever(
      mockAzureUserService
        .getAzureUserByUsername("D6165AD0-AED3-4146-9EF7-222876B57549")
    ).thenReturn(user)

    val token = PreAuthenticatedAuthenticationToken(getAzureUserPersonDetails(), "dummy credentials")

    val returnedUser = azureUserDetailsService.loadUserDetails(token)

    assertThat(returnedUser).isSameAs(user.get())
  }

  private fun getAzureUserPersonDetails(): AzureUserPersonDetails {
    return AzureUserPersonDetails(
      ArrayList(),
      true,
      "D6165AD0-AED3-4146-9EF7-222876B57549",
      "Joe",
      "Bloggs",
      "joe.bloggs@justice.gov.uk",
      true,
      accountNonExpired = true,
      accountNonLocked = true
    )
  }
}

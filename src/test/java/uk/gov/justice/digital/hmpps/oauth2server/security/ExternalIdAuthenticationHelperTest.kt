package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import java.util.*

@Suppress("DEPRECATION")
class ExternalIdAuthenticationHelperTest {
  private val userService: UserService = mock()
  private val helper = ExternalIdAuthenticationHelper(userService)

  @Test
  fun userDetails_notFound() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy { helper.getUserDetails(mapOf("username" to "bobuser")) }
        .isInstanceOf(OAuth2AccessDeniedException::class.java).hasMessage("No user found matching username.")
  }

  @Test
  fun userDetails_found() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(aDeliusUser()))
    val details = helper.getUserDetails(mapOf("username" to "bobuser"))
    assertThat(details).isNotNull()
  }

  private fun aDeliusUser(): DeliusUserPersonDetails {
    return DeliusUserPersonDetails(
        username = "derrick.boom",
        userId = "dboom",
        firstName = "Derrick",
        surname = "Boom",
        email = "derrick@boom.com",
        enabled = true
    )
  }
}

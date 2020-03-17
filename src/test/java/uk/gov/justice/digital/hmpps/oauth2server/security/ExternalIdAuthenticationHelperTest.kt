package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException
import java.util.*

@Suppress("DEPRECATION")
class ExternalIdAuthenticationHelperTest {
  private val userService: UserService = mock()
  private val helper = ExternalIdAuthenticationHelper(userService)

  @Test
  fun userDetails_notFound() {
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy { helper.getUserDetails(mapOf("username" to "bobuser"), false) }
        .isInstanceOf(OAuth2AccessDeniedException::class.java).hasMessage("No user found matching username.")
  }

  @Test
  fun userDetails_found() {
    val details = helper.getUserDetails(mapOf("username" to "bobuser"), true)
    assertThat(details).isNotNull()
  }
}

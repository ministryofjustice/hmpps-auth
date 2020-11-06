@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.AuthorizationRequest

internal class UserSelectorControllerTest {
  private val authentication: Authentication = mock()
  private val authorizationRequest = AuthorizationRequest()

  @Test
  fun userSelector() {
    val model = mutableMapOf("joe" to "bob")
    val modelAndView = UserSelectorController().userSelector(authentication, authorizationRequest, model)
    assertThat(modelAndView.viewName).isEqualTo("userSelector")
    assertThat(modelAndView.model).containsExactly(entry("joe", "bob"))
  }
}

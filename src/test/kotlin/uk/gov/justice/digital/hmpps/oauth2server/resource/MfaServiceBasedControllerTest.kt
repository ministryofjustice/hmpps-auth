@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class MfaServiceBasedControllerTest {
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val mfaServiceBasedController = MfaServiceBasedController(jwtAuthenticationSuccessHandler)
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val authentication: Authentication = mock()

  @Nested
  inner class mfaChallengeRequest {
    @Test
    fun mfaChallengeRequest() {
      val modelAndView = mfaServiceBasedController.mfaChallengeRequest("approval")
      assertThat(modelAndView.viewName).isEqualTo("serviceMfaChallenge")
      assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
        mapOf("smokeCode" to "123456", "user_oauth_approval" to "approval")
      )
    }
  }

  @Nested
  inner class mfaChallenge {
    @Test
    fun mfaChallenge() {
      val view = mfaServiceBasedController.mfaChallenge(request, response, authentication)
      assertThat(view).isEqualTo("forward:/oauth/authorize")
      verify(jwtAuthenticationSuccessHandler).updateMfaInRequest(request, response, authentication)
    }
  }
}

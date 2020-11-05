@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint
import org.springframework.web.bind.support.SessionStatus
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserRetriesService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.util.Optional
import java.util.UUID

internal class UserSelectorAuthorizationEndpointTest {
  private val authorizationEndpoint: AuthorizationEndpoint = mock()
  private val userService: UserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val userRetriesService: UserRetriesService = mock()
  private val endpoint =
    UserSelectorAuthorizationEndpoint(authorizationEndpoint, userService, userRetriesService, telemetryClient)
  private val authentication: OAuth2Authentication = mock()
  private val sessionStatus: SessionStatus = mock()
  private val view: View = mock()

  @Nested
  inner class Authorize {
    @Test
    fun `test authorize no users set`() {
      val authModelAndView = ModelAndView("view")
      whenever(authorizationEndpoint.authorize(any(), any(), any(), any())).thenReturn(authModelAndView)

      val modelAndView = endpoint.authorize(mutableMapOf<String, Any>(), mapOf(), sessionStatus, authentication)

      assertThat(modelAndView).isSameAs(authModelAndView)
    }

    @Test
    fun `test authorize multiple users found`() {
      val authModelAndView = ModelAndView(
        "view",
        mutableMapOf(
          "users" to listOf(
            createSampleUser(username = "user1"),
            createSampleUser(username = "user2"),
          )
        )
      )
      whenever(authorizationEndpoint.authorize(any(), any(), any(), any())).thenReturn(authModelAndView)

      val modelAndView = endpoint.authorize(mutableMapOf<String, Any>(), mapOf(), sessionStatus, authentication)

      assertThat(modelAndView).isSameAs(authModelAndView)
    }

    @Test
    fun `test authorize empty users`() {
      val authModelAndView = ModelAndView("view", mutableMapOf("users" to listOf<String>()))
      whenever(authorizationEndpoint.authorize(any(), any(), any(), any())).thenReturn(authModelAndView)
      whenever(authorizationEndpoint.approveOrDeny(any(), any(), any(), any())).thenReturn(view)

      val modelAndView =
        endpoint.authorize(mutableMapOf<String, Any>(), mapOf(), sessionStatus, authentication)

      assertThat(modelAndView.view).isSameAs(view)
    }

    @Test
    fun `test authorize exactly one user`() {
      val authModelAndView = ModelAndView(
        "view",
        mutableMapOf(
          "users" to listOf(
            User.builder().username("user1").source(AuthSource.auth).build()
          )
        )
      )
      whenever(authorizationEndpoint.authorize(any(), any(), any(), any())).thenReturn(authModelAndView)
      whenever(authorizationEndpoint.approveOrDeny(any(), any(), any(), any())).thenReturn(view)
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.azuread.name, "userid", "jwtId")
      )

      val model = mutableMapOf<String, Any>()
      val modelAndView = endpoint.authorize(model, mapOf(), sessionStatus, authentication)

      assertThat(modelAndView.view).isSameAs(view)
      verify(authorizationEndpoint).approveOrDeny(
        mutableMapOf("user_oauth_approval" to "auth/user1"),
        model,
        sessionStatus,
        authentication
      )
    }
  }

  @Nested
  inner class ApproveOrDeny {
    @Test
    fun `test approveOrDeny no approval specified`() {
      whenever(authorizationEndpoint.approveOrDeny(any(), any(), any(), any())).thenReturn(view)
      val approveView =
        endpoint.approveOrDeny(mutableMapOf(), mutableMapOf<String, String>(), sessionStatus, authentication)
      assertThat(approveView).isSameAs(view)
    }

    @Test
    fun `test approveOrDeny no authentication specified`() {
      whenever(authorizationEndpoint.approveOrDeny(any(), any(), any(), isNull())).thenReturn(view)
      val approveView = endpoint.approveOrDeny(mutableMapOf(), mutableMapOf<String, String>(), sessionStatus, null)
      assertThat(approveView).isSameAs(view)
    }

    @Test
    fun `test approveOrDeny user not found`() {
      whenever(authorizationEndpoint.approveOrDeny(any(), any(), any(), any())).thenReturn(view)
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.azuread.name, "userid", "jwtId")
      )

      val approvalParameters = mutableMapOf("user_oauth_approval" to "none/bloggs")
      val model = mutableMapOf<String, String>()
      val approveView = endpoint.approveOrDeny(approvalParameters, model, sessionStatus, authentication)

      assertThat(approveView).isSameAs(view)
      verify(userService).getMasterUserPersonDetailsWithEmailCheck("bloggs", AuthSource.none, "userid")
      verify(authorizationEndpoint).approveOrDeny(approvalParameters, model, sessionStatus, authentication)
    }

    @Test
    fun `test approveOrDeny user mapped`() {
      whenever(authorizationEndpoint.approveOrDeny(any(), any(), any(), any())).thenReturn(view)
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.azuread.name, "userid", "jwtId")
      )
      val credentials = "some credentials"
      whenever(authentication.credentials).thenReturn(credentials)
      val authorities = setOf(Authority("ROLE_COMMUNITY", "Role Community"))
      val user = User.builder().username("authuser").id(UUID.randomUUID()).person(Person("joe", "bloggs"))
        .authorities(authorities).source(AuthSource.auth).build()
      whenever(userService.getMasterUserPersonDetailsWithEmailCheck(anyString(), any(), anyString())).thenReturn(
        Optional.of(user)
      )

      val approvalParameters = mutableMapOf("user_oauth_approval" to "none/bloggs")
      val model = mutableMapOf<String, String>()
      val approveView = endpoint.approveOrDeny(approvalParameters, model, sessionStatus, authentication)

      assertThat(approveView).isSameAs(view)
      verify(userService).getMasterUserPersonDetailsWithEmailCheck("bloggs", AuthSource.none, "userid")
      verify(authorizationEndpoint).approveOrDeny(
        eq(mutableMapOf("user_oauth_approval" to "true")),
        eq(model),
        eq(sessionStatus),
        check {
          val token = it as UsernamePasswordAuthenticationToken
          assertThat(token.authorities).containsExactlyElementsOf(authorities)
          assertThat(token.credentials).isSameAs(credentials)
          assertThat(token.principal).isEqualTo(
            UserDetailsImpl(
              "authuser",
              "joe bloggs",
              authorities,
              AuthSource.auth.source,
              user.userId,
              "jwtId"
            )
          )
        }
      )
    }

    @Test
    fun `test approveOrDeny create event`() {
      whenever(authorizationEndpoint.approveOrDeny(any(), any(), any(), any())).thenReturn(view)
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.azuread.name, "userid", "jwtId")
      )
      whenever(authentication.credentials).thenReturn("some credentials")
      val user = User.builder().username("authuser").id(UUID.randomUUID()).person(Person("joe", "bloggs"))
        .authorities(setOf(Authority("ROLE_COMMUNITY", "Role Community"))).source(AuthSource.auth).build()
      whenever(userService.getMasterUserPersonDetailsWithEmailCheck(anyString(), any(), anyString())).thenReturn(
        Optional.of(user)
      )

      endpoint.approveOrDeny(
        mutableMapOf("user_oauth_approval" to "none/bloggs"),
        mutableMapOf<String, String>(),
        sessionStatus,
        authentication
      )

      verify(telemetryClient).trackEvent(
        "UserForAccessToken",
        mapOf("azureuser" to "userid", "username" to "authuser", "auth_source" to "auth"),
        null
      )
    }

    @Test
    fun `test approveOrDeny record login`() {
      whenever(authorizationEndpoint.approveOrDeny(any(), any(), any(), any())).thenReturn(view)
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.azuread.name, "userid", "jwtId")
      )
      whenever(authentication.credentials).thenReturn("some credentials")
      val user = User.builder().username("authuser").id(UUID.randomUUID()).person(Person("joe", "bloggs"))
        .authorities(setOf(Authority("ROLE_COMMUNITY", "Role Community"))).source(AuthSource.auth).build()
      whenever(userService.getMasterUserPersonDetailsWithEmailCheck(anyString(), any(), anyString())).thenReturn(
        Optional.of(user)
      )

      endpoint.approveOrDeny(
        mutableMapOf("user_oauth_approval" to "none/bloggs"),
        mutableMapOf<String, String>(),
        sessionStatus,
        authentication
      )

      verify(userRetriesService).resetRetriesAndRecordLogin(user)
    }
  }
}

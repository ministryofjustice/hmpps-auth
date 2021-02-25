@file:Suppress("DEPRECATION", "ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.OAuth2RequestFactory
import org.springframework.security.oauth2.provider.token.TokenStore
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService

internal class UserContextApprovalHandlerTest {
  private val userContextService: UserContextService = mock()
  private val clientDetailsService: ClientDetailsService = mock()
  private val mfaService: MfaService = mock()
  private val handler = UserContextApprovalHandler(userContextService, clientDetailsService, mfaService)
  private val authentication: Authentication = mock()
  private val authorizationRequest = AuthorizationRequest()
  private val requestFactory: OAuth2RequestFactory = mock()
  private val tokenStore: TokenStore = mock()
  private val oAuth2AccessToken: OAuth2AccessToken = mock()
  private val clientDetails: ClientDetails = mock()

  @BeforeEach
  internal fun setUp() {
    handler.setRequestFactory(requestFactory)
    handler.setTokenStore(tokenStore)
    authorizationRequest.clientId = "someClient"
  }

  @Nested
  inner class checkForPreApproval {
    @Test
    fun `not approved as client needs mfa everywhere`() {
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.nomis.name, "userid", "jwtId")
      )
      whenever(clientDetails.additionalInformation).thenReturn(mapOf("mfa" to MfaAccess.all.name))
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)
      whenever(tokenStore.getAccessToken(any())).thenReturn(oAuth2AccessToken)
      whenever(oAuth2AccessToken.isExpired).thenReturn(false)
      val approval = handler.checkForPreApproval(authorizationRequest, authentication)
      assertThat(approval.isApproved).isFalse
    }
    @Test
    fun `not approved as client needs mfa and outside network`() {
      whenever(clientDetails.additionalInformation).thenReturn(mapOf("mfa" to MfaAccess.untrusted.name))
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)
      whenever(mfaService.outsideApprovedNetwork()).thenReturn(true)
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.nomis.name, "userid", "jwtId")
      )
      whenever(tokenStore.getAccessToken(any())).thenReturn(oAuth2AccessToken)
      whenever(oAuth2AccessToken.isExpired).thenReturn(false)
      val approval = handler.checkForPreApproval(authorizationRequest, authentication)
      assertThat(approval.isApproved).isFalse
    }

    @Test
    fun `approved as client needs mfa but inside network`() {
      whenever(clientDetails.additionalInformation).thenReturn(mapOf("mfa" to MfaAccess.untrusted.name))
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId")
      )
      whenever(tokenStore.getAccessToken(any())).thenReturn(oAuth2AccessToken)
      whenever(oAuth2AccessToken.isExpired).thenReturn(false)
      val approval = handler.checkForPreApproval(authorizationRequest, authentication)
      assertThat(approval.isApproved).isTrue
    }

    @Test
    fun `approved as mfa already passed`() {
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId", passedMfa = true)
      )
      whenever(tokenStore.getAccessToken(any())).thenReturn(oAuth2AccessToken)
      whenever(oAuth2AccessToken.isExpired).thenReturn(false)
      val approval = handler.checkForPreApproval(authorizationRequest, authentication)
      assertThat(approval.isApproved).isTrue
    }

    @Test
    fun `not approved as azure ad user`() {
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.azuread.name, "userid", "jwtId", passedMfa = true)
      )
      whenever(tokenStore.getAccessToken(any())).thenReturn(oAuth2AccessToken)
      whenever(oAuth2AccessToken.isExpired).thenReturn(false)
      val approval = handler.checkForPreApproval(authorizationRequest, authentication)
      assertThat(approval.isApproved).isFalse
    }

    @Test
    fun `approved as not azure ad user`() {
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId", passedMfa = true)
      )
      whenever(tokenStore.getAccessToken(any())).thenReturn(oAuth2AccessToken)
      whenever(oAuth2AccessToken.isExpired).thenReturn(false)
      val approval = handler.checkForPreApproval(authorizationRequest, authentication)
      assertThat(approval.isApproved).isTrue
    }
  }

  @Nested
  inner class getUserApprovalRequest {
    @Test
    fun `already passed mfa`() {
      authorizationRequest.requestParameters = mutableMapOf("bob" to "joe")
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId", passedMfa = true)
      )
      val map = handler.getUserApprovalRequest(authorizationRequest, authentication)
      assertThat(map).containsExactly(entry("bob", "joe"))
    }

    @Test
    fun `needs mfa`() {
      authorizationRequest.requestParameters = mutableMapOf("bob" to "joe")
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.azuread.name, "userid", "jwtId")
      )
      whenever(clientDetails.additionalInformation).thenReturn(mapOf("mfa" to MfaAccess.all.name))
      whenever(clientDetailsService.loadClientByClientId(any())).thenReturn(clientDetails)

      val users = listOf(createSampleUser(username = "harry"))
      whenever(userContextService.discoverUsers(any(), any())).thenReturn(users)
      val map = handler.getUserApprovalRequest(authorizationRequest, authentication)
      assertThat(map).containsExactly(entry("bob", "joe"), entry("users", users), entry("requireMfa", true))
    }

    @Test
    fun `not an azuread user`() {
      authorizationRequest.requestParameters = mutableMapOf("bob" to "joe")
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId", passedMfa = true)
      )
      val map = handler.getUserApprovalRequest(authorizationRequest, authentication)
      assertThat(map).containsExactly(entry("bob", "joe"))
    }

    @Test
    fun `an azuread user`() {
      authorizationRequest.requestParameters = mutableMapOf("bob" to "joe")
      whenever(authentication.principal).thenReturn(
        UserDetailsImpl("user", "name", setOf(), AuthSource.azuread.name, "userid", "jwtId", passedMfa = true)
      )
      val users = listOf(createSampleUser(username = "harry"))
      whenever(userContextService.discoverUsers(any(), any())).thenReturn(users)
      val map = handler.getUserApprovalRequest(authorizationRequest, authentication)
      assertThat(map).containsExactly(entry("bob", "joe"), entry("users", users))
    }
  }
}

@file:Suppress("DEPRECATION", "ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.integration

import com.nimbusds.jwt.JWTParser
import net.minidev.json.JSONArray
import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.oauth2server.resource.AzureOIDCExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.RemoteClientExtension

/**
 * Verify clients can login, be redirected back to their system and then logout again.
 * The token-verification spring profile needs to be enabled (as well as the dev profile) for these tests.  This is
 * done automatically in circle configuration for automated builds, but needs enabling when running these tests.  By
 * default the dev profile doesn't have it enabled so that other clients can use this project without issues.
 */
@ExtendWith(RemoteClientExtension::class)
class MfaServiceBasedClientLoginSpecification : AbstractDeliusAuthSpecification() {
  @Page
  internal lateinit var selectUserPage: SelectUserPage

  @Page
  internal lateinit var serviceBasedMfaEmailPage: ServiceBasedMfaEmailPage

  @Page
  internal lateinit var serviceBasedMfaFromAuthorizeEmailPage: ServiceBasedMfaFromAuthorizeEmailPage

  @Test
  fun `Sign in as azure ad user with multiple accounts`() {
    AzureOIDCExtension.azureOIDC.stubToken("Auth_Test@digital.Justice.gov.UK")
    clientAccess(
      {
        loginPage.clickAzureOIDCLink()
        selectUserPage.isAtPage().selectUser("auth", "AUTH_CHANGE_TEST")
        serviceBasedMfaEmailPage.isAtPage().submitCode()
      },
      "service-mfa-test-client"
    )
      .jsonPath(".user_name").isEqualTo("AUTH_CHANGE_TEST")
      .jsonPath(".user_id").isEqualTo("2e285ccd-dcfd-4497-9e22-d6e8e10a2b3f")
      .jsonPath(".sub").isEqualTo("AUTH_CHANGE_TEST")
      .jsonPath(".auth_source").isEqualTo("auth")
      .jsonPath(".access_token").value<JSONArray> {
        val claims = JWTParser.parse(it[0].toString()).jwtClaimsSet
        assertThat(claims.getClaim("user_name")).isEqualTo("AUTH_CHANGE_TEST")
      }
  }

  @Test
  fun `Sign in as nomis user`() {
    clientAccess(
      {
        loginPage.isAtPage().submitLogin("ITAG_USER", "password")
        serviceBasedMfaFromAuthorizeEmailPage.isAtPage().submitCode()
      },
      "service-mfa-test-client"
    )
      .jsonPath(".user_name").isEqualTo("ITAG_USER")
      .jsonPath(".user_id").isEqualTo("1")
      .jsonPath(".sub").isEqualTo("ITAG_USER")
      .jsonPath(".auth_source").isEqualTo("nomis")
  }
}

@PageUrl("/service-mfa-challenge")
class ServiceBasedMfaEmailPage : MfaEmailPage()

// bit of a hack, but can't seem to redirect from the authorize as spring boot ends up wanting to
// append the session id to the url and can't do redirect with session cookie so have to forward instead
@PageUrl("/oauth/authorize")
class ServiceBasedMfaFromAuthorizeEmailPage : MfaEmailPage()

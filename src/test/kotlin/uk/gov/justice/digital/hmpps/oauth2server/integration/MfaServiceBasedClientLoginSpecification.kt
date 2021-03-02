@file:Suppress("DEPRECATION", "ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.integration

import com.nimbusds.jwt.JWTParser
import net.minidev.json.JSONArray
import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.web.reactive.server.WebTestClient
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
  private lateinit var homePage: HomePage

  @Page
  internal lateinit var serviceBasedMfaEmailPage: ServiceBasedMfaEmailPage

  @Page
  internal lateinit var serviceBasedMfaFromAuthorizeEmailPage: ServiceBasedMfaFromAuthorizeEmailPage

  @Page
  internal lateinit var serviceBasedMfaFromAuthorizeTextPage: ServiceBasedMfaFromAuthorizeTextPage

  @Page
  private lateinit var serviceBasedMfaEmailResendCodePage: ServiceBasedMfaEmailResendCodePage

  @Test
  fun `Sign in as azure ad user with multiple accounts`() {
    AzureOIDCExtension.azureOIDC.stubToken("Auth_Test@digital.Justice.gov.UK")
    clientMfaServiceAccess {
      loginPage.clickAzureOIDCLink()
      selectUserPage.isAtPage().selectUser("auth", "AUTH_CHANGE_TEST")
      serviceBasedMfaEmailPage.isAtPage().submitCode()
    }
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
    clientMfaServiceAccess {
      loginPage.isAtPage().submitLogin("ITAG_USER", "password")
      serviceBasedMfaFromAuthorizeEmailPage.isAtPage()
        .assertEmailCodeDestination("itag******@******.gov.uk")
        .submitCode()
    }
      .jsonPath(".user_name").isEqualTo("ITAG_USER")
      .jsonPath(".user_id").isEqualTo("1")
      .jsonPath(".sub").isEqualTo("ITAG_USER")
      .jsonPath(".auth_source").isEqualTo("nomis")
  }

  @Test
  fun `Sign in as auth user with text MFA`() {
    goTo(loginPage).loginAs("AUTH_PREF_TEXT")
    clientMfaServiceAccess {
      serviceBasedMfaFromAuthorizeTextPage.isAtPage()
        .assertMobileCodeDestination("*******0321")
        .submitCode()
    }.jsonPath(".user_name").isEqualTo("AUTH_PREF_TEXT")
  }

  @Test
  fun `Sign in as auth user with secondary email MFA`() {
    goTo(loginPage).loginAs("AUTH_PREF_2ND_EMAIL")
    clientMfaServiceAccess {
      serviceBasedMfaFromAuthorizeEmailPage.isAtPage()
        .assertEmailCodeDestination("jo******@******ith.com")
        .submitCode()
    }.jsonPath(".user_name").isEqualTo("AUTH_PREF_2ND_EMAIL")
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by email`() {
    clientMfaServiceAccess {
      loginPage.isAtPage().submitLogin("ITAG_USER", "password")
      serviceBasedMfaFromAuthorizeEmailPage.isAtPage().resendCodeLink()
      serviceBasedMfaEmailResendCodePage.isAtPage().resendCodeByEmail()
      serviceBasedMfaFromAuthorizeEmailPage.submitCode()
    }
      .jsonPath(".user_name").isEqualTo("ITAG_USER")
  }

  @Test
  fun `Sign in sets mfa passed in jwt cookie to true`() {
    goTo(loginPage).loginAs("AUTH_PREF_TEXT")
    clientMfaServiceAccess {
      serviceBasedMfaFromAuthorizeTextPage.isAtPage()
        .assertMobileCodeDestination("*******0321")
        .submitCode()
    }.jsonPath(".user_name").isEqualTo("AUTH_PREF_TEXT")
    goTo(homePage).isAt()
    val jwt = homePage.parseJwt()
    assertThat(jwt.getBooleanClaim("passed_mfa")).isTrue
  }

  @Test
  fun `Login as user with MFA enabled but no email addresses or mobile number`() {
    goTo(loginPage).loginAsWithUnverifiedEmail("AUTH_NO_PREF")
      .cancel()

    startClientAccess("service-mfa-test-client")
    homePage.isAtError()
      .checkError(
        """
            We need to send you a security code, but we can't find a verified email address or phone number. 
            Please verify your email address by clicking the link in your email.
        """.trimIndent().replace("\n", "")
      )
  }

  private fun clientMfaServiceAccess(doWithinAuth: () -> Unit): WebTestClient.BodyContentSpec =
    clientAccess(doWithinAuth, "service-mfa-test-client")
}

@PageUrl("/service-mfa-challenge")
class ServiceBasedMfaEmailPage : MfaEmailPage()

// bit of a hack, but can't seem to redirect from the authorize as spring boot ends up wanting to
// append the session id to the url and can't do redirect with session cookie so have to forward instead
@PageUrl("/oauth/authorize")
class ServiceBasedMfaFromAuthorizeEmailPage : MfaEmailPage()

@PageUrl("/oauth/authorize")
class ServiceBasedMfaFromAuthorizeTextPage : MfaTextPage()

@PageUrl("/service-mfa-resend")
class ServiceBasedMfaTextResendCodePage : MfaTextResendCodePage()

@PageUrl("/service-mfa-resend")
class ServiceBasedMfaEmailResendCodePage : MfaEmailResendCodePage()

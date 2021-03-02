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
  internal lateinit var mfaEmailPage: ServiceBasedMfaEmailPage

  @Page
  internal lateinit var mfaTextPage: ServiceBasedMfaTextPage

  @Page
  private lateinit var mfaEmailResendCodePage: ServiceBasedMfaEmailResendCodePage

  @Test
  fun `Sign in as azure ad user with multiple accounts`() {
    AzureOIDCExtension.azureOIDC.stubToken("Auth_Test@digital.Justice.gov.UK")
    clientMfaServiceAccess {
      loginPage.clickAzureOIDCLink()
      selectUserPage.isAtPage().selectUser("auth", "AUTH_CHANGE_TEST")
      mfaEmailPage.isAtPage().submitCode()
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
      mfaEmailPage.isAtPage()
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
      mfaTextPage.isAtPage()
        .assertMobileCodeDestination("*******0321")
        .submitCode()
    }.jsonPath(".user_name").isEqualTo("AUTH_PREF_TEXT")
  }

  @Test
  fun `Sign in as auth user with secondary email MFA`() {
    goTo(loginPage).loginAs("AUTH_PREF_2ND_EMAIL")
    clientMfaServiceAccess {
      mfaEmailPage.isAtPage()
        .assertEmailCodeDestination("jo******@******ith.com")
        .submitCode()
    }.jsonPath(".user_name").isEqualTo("AUTH_PREF_2ND_EMAIL")
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by email`() {
    clientMfaServiceAccess {
      loginPage.isAtPage().submitLogin("ITAG_USER", "password")
      mfaEmailPage.isAtPage().resendCodeLink()
      mfaEmailResendCodePage.isAtPage().resendCodeByEmail()
      mfaEmailPage.isAtPage()
        .assertEmailCodeDestination("itag******@******.gov.uk")
        .submitCode()
    }
      .jsonPath(".user_name").isEqualTo("ITAG_USER")
  }

  @Test
  fun `Mfa preference email - I would like the MFA code to be resent by text`() {
    clientMfaServiceAccess {
      loginPage.isAtPage().submitLogin("AUTH_PREF_EMAIL_TEXT")
      mfaEmailPage.isAtPage().resendCodeLink()
      mfaEmailResendCodePage.isAtPage().resendCodeByText()
      mfaTextPage.isAtPage()
        .assertMobileCodeDestination("*******0321")
        .submitCode()
    }
      .jsonPath(".user_name").isEqualTo("AUTH_PREF_EMAIL_TEXT")
  }

  @Test
  fun `Sign in sets mfa passed in jwt cookie to true`() {
    goTo(loginPage).loginAs("AUTH_PREF_TEXT")
    clientMfaServiceAccess {
      mfaTextPage.isAtPage()
        .assertMobileCodeDestination("*******0321")
        .submitCode()
    }.jsonPath(".user_name").isEqualTo("AUTH_PREF_TEXT")
    goTo(homePage).isAt()
    val jwt = homePage.parseJwt()
    assertThat(jwt.getBooleanClaim("passed_mfa")).isTrue
  }

  @Test
  fun `MFA code is required - text message`() {
    goTo(loginPage).loginAs("AUTH_PREF_TEXT")
    startClientAccess("service-mfa-test-client")
    mfaTextPage.isAtPage()
      .submitCode(" ")
    mfaTextPage.checkError("Enter the code received in the text message")
      .submitCode("123")
      .checkTextCodeIsIncorrectError()
  }

  @Test
  fun `MFA code is required - email`() {
    goTo(loginPage).loginAs("AUTH_PREF_2ND_EMAIL")
    startClientAccess("service-mfa-test-client")
    mfaEmailPage.isAtPage()
      .submitCode(" ")
    mfaEmailPage.enterTheCodeError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
  }

  @Test
  fun `MFA user email preference gets locked after 3 invalid MFA attempts`() {
    goTo(loginPage).loginAs("AUTH_MFA_LOCKED6_EMAIL")
    startClientAccess("service-mfa-test-client")
    mfaEmailPage.isAtPage()
      .submitCode("123")
    mfaEmailPage.checkEmailCodeIsIncorrectError()
      .submitCode("123")
      .checkEmailCodeIsIncorrectError()
      .submitCode("123")

    loginPage.checkLoginAccountLockedError()
      .loginError("AUTH_MFA_LOCKED6_EMAIL")
      .checkLoginAccountLockedError()

    // ensure user is actually now logged out
    goTo(homePage)
    loginPage.isAtPage()
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

  @Test
  fun `Sign in as user with email MFA enabled doesn't prompt twice for MFA`() {
    goTo(loginPage).loginWithMfaEmail("AUTH_MFA_USER")
      .assertEmailCodeDestination("mfa_******@******.gov.uk")
      .submitCode()
    homePage.isAt()

    clientMfaServiceAccess()
      .jsonPath(".user_name").isEqualTo("AUTH_MFA_USER")
  }

  private fun clientMfaServiceAccess(doWithinAuth: () -> Unit = {}): WebTestClient.BodyContentSpec =
    clientAccess("service-mfa-test-client", doWithinAuth)
}

@PageUrl("/service-mfa-challenge")
class ServiceBasedMfaEmailPage : MfaEmailPage()

@PageUrl("/service-mfa-challenge")
class ServiceBasedMfaTextPage : MfaTextPage()

@PageUrl("/service-mfa-resend")
class ServiceBasedMfaEmailResendCodePage : MfaEmailResendCodePage()

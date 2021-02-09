package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetailsHelper
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.LinkAndEmail
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientException
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class VerifyEmailControllerTest {
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val userService: UserService = mock()
  private val tokenService: TokenService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val verifyEmailController = VerifyEmailController(
    jwtAuthenticationSuccessHandler,
    verifyEmailService,
    userService,
    tokenService,
    telemetryClient,
    false
  )
  private val verifyEmailControllerSmokeTestEnabled = VerifyEmailController(
    jwtAuthenticationSuccessHandler,
    verifyEmailService,
    userService,
    tokenService,
    telemetryClient,
    true
  )
  private val principal = UsernamePasswordAuthenticationToken("user", "pass")

  @Test
  fun verifyEmailRequest() {
    val emails = listOf("bob")
    whenever(verifyEmailService.getExistingEmailAddressesForUsername(anyString())).thenReturn(emails)
    val modelAndView = verifyEmailController.verifyEmailRequest(principal, request, response, null)
    assertThat(modelAndView?.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView?.model).containsExactly(entry("candidates", emails))
  }

  @Test
  fun verifyEmailRequest_existingUserEmail() {
    val user = createSampleUser(username = "bob", email = "email")
    whenever(verifyEmailService.getEmail(anyString())).thenReturn(Optional.of(user))
    val modelAndView = verifyEmailController.verifyEmailRequest(principal, request, response, null)
    assertThat(modelAndView?.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView?.model).containsExactly(entry("suggestion", user.email))
  }

  @Test
  fun verifyEmailRequest_existingUserEmailVerified() {
    val user = createSampleUser(username = "bob", verified = true)
    whenever(verifyEmailService.getEmail(anyString())).thenReturn(Optional.of(user))
    SecurityContextHolder.getContext().authentication = principal
    val modelAndView = verifyEmailController.verifyEmailRequest(principal, request, response, null)
    assertThat(modelAndView).isNull()
    verify(jwtAuthenticationSuccessHandler).proceed(request, response, principal)
  }

  @Test
  fun verifyEmailContinue() {
    SecurityContextHolder.getContext().authentication = principal
    verifyEmailController.verifyEmailContinue(request, response)
    verify(jwtAuthenticationSuccessHandler).proceed(request, response, principal)
  }

  @Test
  fun verifyEmailSkip() {
    SecurityContextHolder.getContext().authentication = principal
    verifyEmailController.verifyEmailSkip(request, response)
    verify(jwtAuthenticationSuccessHandler).proceed(request, response, principal)
    verify(telemetryClient).trackEvent("VerifyEmailRequestSkip", mapOf(), null)
  }

  @Test
  fun verifyEmail_noselection() {
    val candidates = listOf("joe", "bob")
    whenever(verifyEmailService.getExistingEmailAddressesForUsername(anyString())).thenReturn(candidates)
    val modelAndView = verifyEmailController.verifyEmail("", "", EmailType.PRIMARY, null, principal, request, response)
    assertThat(modelAndView?.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView?.model).containsExactly(entry("error", "noselection"), entry("candidates", candidates))
  }

  @Test
  fun verifyEmail_Exception() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(getUserPersonalDetails()))
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(createSampleUser()))
    whenever(
      verifyEmailService.changeEmailAndRequestVerification(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq(EmailType.PRIMARY)
      )
    ).thenThrow(NotificationClientException("something went wrong"))
    val modelAndView =
      verifyEmailController.verifyEmail("a@b.com", null, EmailType.PRIMARY, null, principal, request, response)
    assertThat(modelAndView?.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView?.model).containsExactly(entry("email", "a@b.com"), entry("error", "other"))
  }

  @Test
  fun verifyEmail_Success() {
    whenever(
      verifyEmailService.changeEmailAndRequestVerification(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq(EmailType.PRIMARY)
      )
    ).thenReturn(LinkAndEmail("link", "newemail@justice.gov.uk"))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(getUserPersonalDetails()))
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    val email = "o'there+bob@b-c.d"
    val modelAndView =
      verifyEmailControllerSmokeTestEnabled.verifyEmail(
        "other",
        email,
        EmailType.PRIMARY,
        null,
        principal,
        request,
        response
      )
    assertThat(modelAndView?.viewName).isEqualTo("verifyEmailSent")
    assertThat(modelAndView?.model).containsOnly(
      entry("verifyLink", "link"),
      entry("emailType", EmailType.PRIMARY),
      entry("email", "newemail@justice.gov.uk")
    )
    verify(verifyEmailService).changeEmailAndRequestVerification(
      "user",
      email,
      "Bob",
      "Bob Last",
      "http://some.url-confirm?token=",
      EmailType.PRIMARY
    )
  }

  @Test
  fun verifyEmail_ChangeJwt() {
    whenever(
      verifyEmailService.changeEmailAndRequestVerification(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq(EmailType.PRIMARY)
      )
    ).thenReturn(LinkAndEmail("link", "newemail@justice.gov.uk"))
    val user = getUserPersonalDetails()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    val email = "o'there+bob@b-c.d"
    val emailPrincipal = UsernamePasswordAuthenticationToken("user@somewhere.com", "pass")

    verifyEmailControllerSmokeTestEnabled.verifyEmail(
      "other",
      email,
      EmailType.PRIMARY,
      null,
      emailPrincipal,
      request,
      response
    )

    verify(jwtAuthenticationSuccessHandler).updateAuthenticationInRequest(
      eq(request),
      eq(response),
      check { assertThat(it.principal).isEqualTo(user) }
    )
  }

  @Test
  fun verifyEmail_NoChangeJwt() {
    whenever(
      verifyEmailService.changeEmailAndRequestVerification(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq(EmailType.PRIMARY)
      )
    ).thenReturn(LinkAndEmail("link", "newemail@justice.gov.uk"))
    val user = getUserPersonalDetails()
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    val email = "o'there+bob@b-c.d"

    verifyEmailControllerSmokeTestEnabled.verifyEmail(
      "other",
      email,
      EmailType.PRIMARY,
      null,
      principal,
      request,
      response
    )

    verifyZeroInteractions(jwtAuthenticationSuccessHandler)
  }

  @Test
  fun verifyEmailConfirm() {
    whenever(verifyEmailService.confirmEmail(anyString())).thenReturn(Optional.empty())
    val modelAndView = verifyEmailController.verifyEmailConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailSuccess")
    assertThat(modelAndView.model).containsOnly(entry("emailType", "PRIMARY"))
  }

  @Test
  fun verifyEmail_AlreadyVerified() {
    whenever(userService.isSameAsCurrentVerifiedEmail(anyString(), anyString(), eq(EmailType.PRIMARY))).thenReturn(true)
    val modelAndView = verifyEmailController.verifyEmail(
      "change",
      "auth_email@digital.justice.gov.uk",
      EmailType.PRIMARY,
      null,
      principal,
      request,
      response
    )
    assertThat(modelAndView?.viewName).isEqualTo("redirect:/verify-email-already")
  }

  @Test
  fun verifyEmailConfirm_Failure() {
    whenever(verifyEmailService.confirmEmail(anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = verifyEmailController.verifyEmailConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-email-failure")
  }

  @Test
  fun verifySecondaryEmailConfirm_Failure() {
    whenever(verifyEmailService.confirmSecondaryEmail(anyString())).thenReturn(Optional.of("invalid"))
    val modelAndView = verifyEmailController.verifySecondaryEmailConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-email-failure")
  }

  @Test
  fun verifySecondaryEmail_noselection() {
    val candidates = listOf("joe", "bob")
    whenever(verifyEmailService.getExistingEmailAddressesForUsername(anyString())).thenReturn(candidates)
    val modelAndView =
      verifyEmailController.verifyEmail("", "", EmailType.SECONDARY, null, principal, request, response)
    assertThat(modelAndView?.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView?.model).containsExactly(entry("error", "noselection"), entry("candidates", candidates))
  }

  @Test
  fun verifySecondaryEmail_Exception() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(getUserPersonalDetails()))
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(createSampleUser()))
    whenever(
      verifyEmailService.changeEmailAndRequestVerification(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq(EmailType.SECONDARY)
      )
    ).thenThrow(NotificationClientException("something went wrong"))
    val modelAndView =
      verifyEmailController.verifyEmail(
        "a@b.com",
        null,
        EmailType.SECONDARY,
        "71396b28-0c57-4efd-bc70-cc5992965aed",
        principal,
        request,
        response
      )
    assertThat(modelAndView?.viewName).isEqualTo("redirect:/new-backup-email?token=71396b28-0c57-4efd-bc70-cc5992965aed")
    assertThat(modelAndView?.model).containsExactly(
      entry("email", "a@b.com"),
      entry("error", "other")
    )
  }

  @Test
  fun verifySecondaryEmail_Success() {
    whenever(
      verifyEmailService.changeEmailAndRequestVerification(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq(EmailType.SECONDARY)
      )
    ).thenReturn(LinkAndEmail("link", "newemail@justice.gov.uk"))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(getUserPersonalDetails()))
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    val email = "o'there+bob@b-c.d"
    val modelAndView = verifyEmailControllerSmokeTestEnabled.verifyEmail(
      "other",
      email,
      EmailType.SECONDARY,
      null,
      principal,
      request,
      response
    )
    assertThat(modelAndView?.viewName).isEqualTo("verifyEmailSent")
    assertThat(modelAndView?.model).containsOnly(
      entry("verifyLink", "link"),
      entry("emailType", EmailType.SECONDARY),
      entry("email", "newemail@justice.gov.uk")
    )
    verify(verifyEmailService).changeEmailAndRequestVerification(
      "user",
      email,
      "Bob",
      "Bob Last",
      "http://some.url-secondary-confirm?token=",
      EmailType.SECONDARY
    )
  }

  @Test
  fun verifySecondaryEmailConfirm() {
    whenever(verifyEmailService.confirmSecondaryEmail(anyString())).thenReturn(Optional.empty())
    val modelAndView = verifyEmailController.verifySecondaryEmailConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailSuccess")
    assertThat(modelAndView.model).containsExactly(entry("emailType", "SECONDARY"))
  }

  @Test
  fun verifySecondaryEmail_AlreadyVerified() {
    whenever(userService.isSameAsCurrentVerifiedEmail(anyString(), anyString(), eq(EmailType.SECONDARY))).thenReturn(
      true
    )
    val modelAndView = verifyEmailController.verifyEmail(
      "change",
      "auth_email@digital.justice.gov.uk",
      EmailType.SECONDARY,
      null,
      principal,
      request,
      response
    )
    assertThat(modelAndView?.viewName).isEqualTo("redirect:/verify-email-already")
  }

  private fun getUserPersonalDetails(): NomisUserPersonDetails =
    NomisUserPersonDetailsHelper.createSampleNomisUser(
      staff = Staff(
        firstName = "bob",
        status = "INACTIVE",
        lastName = "last",
        staffId = 1
      )
    )

  @Test
  fun secondaryEmailResendRequest_notVerified() {
    whenever(verifyEmailService.secondaryEmailVerified(anyString())).thenReturn(false)
    val view = verifyEmailController.secondaryEmailResendRequest(principal)
    assertThat(view).isEqualTo("redirect:/verify-secondary-email-resend")
  }

  @Test
  fun secondaryEmailResendRequest_alreadyVerified() {
    whenever(verifyEmailService.secondaryEmailVerified(anyString())).thenReturn(true)
    val view = verifyEmailController.secondaryEmailResendRequest(principal)
    assertThat(view).isEqualTo("redirect:/verify-email-already")
  }

  @Test
  fun `secondaryEmailResend send Code`() {
    whenever(userService.getUser(anyString())).thenReturn(
      createSampleUser(secondaryEmail = "someemail")
    )
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    whenever(verifyEmailService.resendVerificationCodeSecondaryEmail(anyString(), anyString()))
      .thenReturn(Optional.of("http://some.url/auth/verify-email-secondary-confirm?token=71396b28-0c57-4efd-bc70-cc5992965aed"))
    val modelAndView =
      verifyEmailController.secondaryEmailResend("71396b28-0c57-4efd-bc70-cc5992965aed", principal, request)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-email-sent")
    assertThat(modelAndView.model).isEmpty()
  }

  @Test
  fun `secondaryEmailResend send code smoke enabled`() {

    whenever(userService.getUser(anyString())).thenReturn(
      createSampleUser(secondaryEmail = "someemail")
    )
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    whenever(verifyEmailService.resendVerificationCodeSecondaryEmail(anyString(), anyString()))
      .thenReturn(Optional.of("http://some.url/auth/verify-email-secondary-confirm?token=71396b28-0c57-4efd-bc70-cc5992965aed"))
    val modelAndView = verifyEmailControllerSmokeTestEnabled.secondaryEmailResend(
      "71396b28-0c57-4efd-bc70-cc5992965aed",
      principal,
      request
    )
    assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-email-sent")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(mapOf("verifyLink" to "http://some.url/auth/verify-email-secondary-confirm?token=71396b28-0c57-4efd-bc70-cc5992965aed"))
  }

  @Test
  fun secondaryEmailResend_verifyMobileException() {
    whenever(userService.getUser(anyString())).thenReturn(
      createSampleUser(secondaryEmail = "someemail")
    )
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    whenever(verifyEmailService.resendVerificationCodeSecondaryEmail(anyString(), anyString())).thenThrow(
      VerifyEmailException("reason")
    )
    val modelAndView =
      verifyEmailController.secondaryEmailResend("71396b28-0c57-4efd-bc70-cc5992965aed", principal, request)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/new-backup-email?token=71396b28-0c57-4efd-bc70-cc5992965aed")
    assertThat(modelAndView.model).containsExactly(
      entry("email", null),
      entry("error", "reason")
    )
    verify(telemetryClient).trackEvent(
      eq("VerifyEmailRequestFailure"),
      check {
        assertThat(it).containsExactlyInAnyOrderEntriesOf(mapOf("username" to "user", "reason" to "reason"))
      },
      isNull()
    )
  }

  @Test
  fun secondaryEmailResend_notificationClientException() {
    whenever(userService.getUser(anyString())).thenReturn(
      createSampleUser(secondaryEmail = "someemail")
    )
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    whenever(verifyEmailService.resendVerificationCodeSecondaryEmail(anyString(), anyString())).thenThrow(
      NotificationClientException("something went wrong")
    )
    val modelAndView =
      verifyEmailController.secondaryEmailResend("71396b28-0c57-4efd-bc70-cc5992965aed", principal, request)
    assertThat(modelAndView.viewName).isEqualTo("redirect:/new-backup-email?token=71396b28-0c57-4efd-bc70-cc5992965aed")
    assertThat(modelAndView.model).containsExactly(
      entry("email", null),
      entry("error", "other")
    )
  }

  @Test
  fun `verify email link expired`() {
    whenever(verifyEmailService.confirmEmail(anyString())).thenReturn(Optional.of("expired"))
    val modelAndView = verifyEmailController.verifyEmailConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-email-expired")
    assertThat(modelAndView.model).containsOnly(entry("token", "token"))
  }

  @Test
  fun `verify secondary email link expired`() {
    whenever(verifyEmailService.confirmSecondaryEmail(anyString())).thenReturn(Optional.of("expired"))
    val modelAndView = verifyEmailController.verifySecondaryEmailConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("redirect:/verify-email-secondary-expired")
    assertThat(modelAndView.model).containsOnly(entry("token", "token"))
  }

  @Test
  fun `verify email link expired link resend`() {
    whenever(tokenService.getUserFromToken(UserToken.TokenType.VERIFIED, "token"))
      .thenReturn(createSampleUser(email = "bob@digital.justice.gov.uk", username = "bob"))
    whenever(request.requestURL)
      .thenReturn(StringBuffer("http://some.url/expired"))
    whenever(verifyEmailService.resendVerificationCodeEmail(anyString(), anyString()))
      .thenReturn(Optional.of("verifyLink"))
    val modelAndView = verifyEmailController.verifyEmailLinkExpired("token", request)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailExpired")
    assertThat(modelAndView.model).containsOnly(entry("email", "b******@******.gov.uk"))
  }

  @Test
  fun `verify email link expired link resend smoke test enabled`() {
    whenever(tokenService.getUserFromToken(UserToken.TokenType.VERIFIED, "token"))
      .thenReturn(createSampleUser(username = "bob", email = "bob@digital.justice.gov.uk"))
    whenever(request.requestURL)
      .thenReturn(StringBuffer("http://some.url/expired"))
    whenever(verifyEmailService.resendVerificationCodeEmail(anyString(), anyString()))
      .thenReturn(Optional.of("verifyLink"))
    val modelAndView = verifyEmailControllerSmokeTestEnabled.verifyEmailLinkExpired("token", request)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailExpired")
    assertThat(modelAndView.model).containsExactly(entry("email", "b******@******.gov.uk"), entry("link", "verifyLink"))
  }

  @Test
  fun `verify secondary email link expired link resend`() {
    whenever(tokenService.getUserFromToken(UserToken.TokenType.SECONDARY, "token"))
      .thenReturn(createSampleUser(username = "bob"))
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/expired"))
    whenever(
      verifyEmailService.resendVerificationCodeSecondaryEmail(
        anyString(),
        anyString()
      )
    ).thenReturn(Optional.of("verifyLink"))
    whenever(verifyEmailService.maskedSecondaryEmailFromUsername(anyString())).thenReturn("b******@******ail.com")
    val modelAndView = verifyEmailController.verifySecondaryEmailLinkExpired("token", request)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailExpired")
    assertThat(modelAndView.model).containsOnly(entry("email", "b******@******ail.com"))
  }

  @Test
  fun `verify secondary email link expired link resend smoke test enabled`() {
    whenever(tokenService.getUserFromToken(UserToken.TokenType.SECONDARY, "token"))
      .thenReturn(createSampleUser(username = "bob"))
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url/expired"))
    whenever(
      verifyEmailService.resendVerificationCodeSecondaryEmail(
        anyString(),
        anyString()
      )
    ).thenReturn(Optional.of("verifyLink"))
    whenever(verifyEmailService.maskedSecondaryEmailFromUsername(anyString())).thenReturn("b******@******ail.com")
    val modelAndView = verifyEmailControllerSmokeTestEnabled.verifySecondaryEmailLinkExpired("token", request)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailExpired")
    assertThat(modelAndView.model).containsExactly(entry("email", "b******@******ail.com"), entry("link", "verifyLink"))
  }
}

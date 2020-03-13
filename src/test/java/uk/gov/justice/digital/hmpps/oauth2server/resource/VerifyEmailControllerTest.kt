package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.service.notify.NotificationClientException
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class VerifyEmailControllerTest {
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val userService: UserService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val verifyEmailController = VerifyEmailController(jwtAuthenticationSuccessHandler, verifyEmailService, userService, telemetryClient, true)
  private val principal = UsernamePasswordAuthenticationToken("user", "pass")

  @Test
  fun verifyEmailRequest() {
    val emails = listOf("bob")
    whenever(verifyEmailService.getExistingEmailAddresses(anyString())).thenReturn(emails)
    val modelAndView = verifyEmailController.verifyEmailRequest(principal, request, response, null)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView.model).containsExactly(entry("candidates", emails))
  }

  @Test
  fun verifyEmailRequest_existingUserEmail() {
    val user = User.of("bob")
    user.email = "email"
    whenever(verifyEmailService.getEmail(anyString())).thenReturn(Optional.of(user))
    val modelAndView = verifyEmailController.verifyEmailRequest(principal, request, response, null)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView.model).containsExactly(entry("suggestion", user.email))
  }

  @Test
  fun verifyEmailRequest_existingUserEmailVerified() {
    val user = User.of("bob")
    user.isVerified = true
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
    whenever(verifyEmailService.getExistingEmailAddresses(anyString())).thenReturn(candidates)
    val modelAndView = verifyEmailController.verifyEmail("", "", principal, request, response)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView.model).containsExactly(entry("error", "noselection"), entry("candidates", candidates))
  }

  @Test
  fun verifyEmail_Exception() {
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(getUserPersonalDetails()))
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(User()))
    whenever(verifyEmailService.requestVerification(anyString(), anyString(), anyString(), anyString())).thenThrow(NotificationClientException("something went wrong"))
    val modelAndView = verifyEmailController.verifyEmail("a@b.com", null, principal, request, response)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmail")
    assertThat(modelAndView.model).containsExactly(entry("email", "a@b.com"), entry("error", "other"))
  }

  @Test
  fun verifyEmail_Success() {
    whenever(verifyEmailService.requestVerification(anyString(), anyString(), anyString(), anyString())).thenReturn("link")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(getUserPersonalDetails()))
    whenever(request.requestURL).thenReturn(StringBuffer("http://some.url"))
    val email = "o'there+bob@b-c.d"
    val modelAndView = verifyEmailController.verifyEmail("other", email, principal, request, response)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailSent")
    assertThat(modelAndView.model).containsExactly(entry("verifyLink", "link"), entry("email", email))
    verify(verifyEmailService).requestVerification("user", email, "Bob", "http://some.url-confirm?token=")
  }

  @Test
  fun verifyEmailConfirm() {
    whenever(verifyEmailService.confirmEmail(anyString())).thenReturn(Optional.empty())
    val modelAndView = verifyEmailController.verifyEmailConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailSuccess")
    assertThat(modelAndView.model).isEmpty()
  }

  @Test
  fun verifyEmail_AlreadyVerified() {
    whenever(userService.isSameAsCurrentVerifiedEmail(anyString(), anyString())).thenReturn(true)
    val modelAndView = verifyEmailController.verifyEmail("change", "auth_email@digital.justice.gov.uk", principal, request, response)
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailAlready")
  }

  @Test
  fun verifyEmailConfirm_Failure() {
    whenever(verifyEmailService.confirmEmail(anyString())).thenReturn(Optional.of("failed"))
    val modelAndView = verifyEmailController.verifyEmailConfirm("token")
    assertThat(modelAndView.viewName).isEqualTo("verifyEmailFailure")
    assertThat(modelAndView.model).containsExactly(entry("error", "failed"))
  }

  private fun getUserPersonalDetails(): NomisUserPersonDetails {
    val account = NomisUserPersonDetails()
    val staff = Staff()
    account.staff = staff
    account.username = "user"
    staff.firstName = "bob"
    return account
  }
}

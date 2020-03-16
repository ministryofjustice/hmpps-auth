package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider.MfaUnavailableException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserRetriesService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.service.notify.NotificationClientApi
import java.util.*

class MfaServiceTest {
  private val tokenService: TokenService = mock()
  private val userService: UserService = mock()
  private val userRetriesService: UserRetriesService = mock()
  private val notificationClientApi: NotificationClientApi = mock()
  private val request = MockHttpServletRequest()
  private val service = MfaService(setOf("12.21.23.24"), setOf("MFA"), "emailTemplate", "textTemplate", tokenService, userService, notificationClientApi, userRetriesService)

  @BeforeEach
  fun setUp() {
    request.remoteAddr = "0:0:0:0:0:0:0:1"
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request, null))
  }

  @AfterEach
  fun tearDown() {
    RequestContextHolder.resetRequestAttributes()
  }

  @Test
  fun `needsMfa whitelisted IP`() {
    request.remoteAddr = "12.21.23.24"

    assertThat(service.needsMfa(emptySet())).isFalse()
  }

  @Test
  fun `needsMfa non whitelisted IP`() {
    assertThat(service.needsMfa(emptySet())).isFalse()
  }

  @Test
  fun `needsMfa non whitelisted IP enabled role`() {
    assertThat(service.needsMfa(setOf(SimpleGrantedAuthority("MFA")))).isTrue()
  }

  @Test
  fun `validateAndRemoveMfaCode null`() {
    assertThatThrownBy { service.validateAndRemoveMfaCode("", null) }.isInstanceOf(MfaFlowException::class.java).withFailMessage("missingcode")
  }

  @Test
  fun `validateAndRemoveMfaCode blank`() {
    assertThatThrownBy { service.validateAndRemoveMfaCode("", "   ") }.isInstanceOf(MfaFlowException::class.java).withFailMessage("missingcode")
  }

  @Test
  fun `validateAndRemoveMfaCode token error`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("bob")))

    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("someproblem"))
    assertThatThrownBy { service.validateAndRemoveMfaCode("", "somecode") }.isInstanceOf(MfaFlowException::class.java).withFailMessage("someproblem")
  }

  @Test
  fun `validateAndRemoveMfaCode success`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("bob")))
    service.validateAndRemoveMfaCode("sometoken", "somecode")
  }

  @Test
  fun `validateAndRemoveMfaCode success get token call`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("bob")))
    service.validateAndRemoveMfaCode("sometoken", "somecode")

    verify(tokenService).getToken(TokenType.MFA, "sometoken")
  }

  @Test
  fun `validateAndRemoveMfaCode success check token call`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("bob")))
    service.validateAndRemoveMfaCode("sometoken", "somecode")

    verify(tokenService).checkToken(TokenType.MFA_CODE, "somecode")
  }

  @Test
  fun `validateAndRemoveMfaCode success find master details`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("bob")))
    service.validateAndRemoveMfaCode("sometoken", "somecode")

    verify(userService).findMasterUserPersonDetails("user")
  }

  @Test
  fun `validateAndRemoveMfaCode success remove tokens`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("bob")))
    service.validateAndRemoveMfaCode("sometoken", "somecode")

    verify(tokenService).removeToken(TokenType.MFA, "sometoken")
    verify(tokenService).removeToken(TokenType.MFA_CODE, "somecode")
  }

  @Test
  fun `validateAndRemoveMfaCode success reset retries `() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.of("bob")))
    service.validateAndRemoveMfaCode("sometoken", "somecode")

    verify(userRetriesService).resetRetries("bob")
  }

  @Test
  fun `validateAndRemoveMfaCode account locked`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(User.builder().username("bob").locked(true).build()))
    assertThatThrownBy { service.validateAndRemoveMfaCode("sometoken", "somecode") }.isInstanceOf(LoginFlowException::class.java).withFailMessage("locked")
  }

  @Test
  fun `createTokenAndSendMfaCode success`() {
    val user = User.builder().username("bob").person(Person("first", "last")).email("email").verified(true).build()
    whenever(userService.getOrCreateUser(anyString())).thenReturn(user)
    whenever(tokenService.createToken(eq(TokenType.MFA), anyString())).thenReturn("sometoken")
    whenever(tokenService.createToken(eq(TokenType.MFA_CODE), anyString())).thenReturn("somecode")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    assertThat(service.createTokenAndSendMfaCode("user")).isEqualTo(MfaData("sometoken", "somecode", MfaPreferenceType.EMAIL))
  }

  @Test
  fun `createTokenAndSendMfaCode by Email check email params`() {
    val user = User.builder().username("bob").person(Person("first", "last")).email("email").verified(true).build()
    whenever(userService.getOrCreateUser(anyString())).thenReturn(user)
    whenever(tokenService.createToken(eq(TokenType.MFA), anyString())).thenReturn("sometoken")
    whenever(tokenService.createToken(eq(TokenType.MFA_CODE), anyString())).thenReturn("somecode")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    service.createTokenAndSendMfaCode("user")

    verify(notificationClientApi).sendEmail("emailTemplate", "email", mapOf("firstName" to "first", "code" to "somecode"), null)
  }

  @Test
  fun `createTokenAndSendMfaCode by text check text params`() {
    val user = User.builder().username("bob").mobile("07700900321").mobileVerified(true).mfaPreference(MfaPreferenceType.TEXT).build()
    whenever(userService.getOrCreateUser(anyString())).thenReturn(user)
    whenever(tokenService.createToken(eq(TokenType.MFA), anyString())).thenReturn("sometoken")
    whenever(tokenService.createToken(eq(TokenType.MFA_CODE), anyString())).thenReturn("somecode")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    service.createTokenAndSendMfaCode("user")

    verify(notificationClientApi).sendSms("textTemplate", "07700900321", mapOf("mfaCode" to "somecode"), null, null)
  }

  @Test
  fun `createTokenAndSendMfaCode no valid preference`() {
    val user = User.builder().username("bob").build()
    whenever(userService.getOrCreateUser(anyString())).thenReturn(user)
    whenever(tokenService.createToken(eq(TokenType.MFA), anyString())).thenReturn("sometoken")
    whenever(tokenService.createToken(eq(TokenType.MFA_CODE), anyString())).thenReturn("somecode")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    assertThatThrownBy { service.createTokenAndSendMfaCode("user") }.isInstanceOf(MfaUnavailableException::class.java)
  }

  @Test
  fun `resendMfaCode no code`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))

    val code = service.resendMfaCode("sometoken", MfaPreferenceType.EMAIL)
    assertThat(code).isEqualTo(null)
    verify(userService, never()).findMasterUserPersonDetails(anyString())
  }

  @Test
  fun `resendMfaCode check code`() {
    val user = User.of("user")
    val userToken = user.createToken(TokenType.MFA)
    val userCode = user.createToken(TokenType.MFA_CODE)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    assertThat(service.resendMfaCode("sometoken", MfaPreferenceType.EMAIL)).isEqualTo(userCode.token)
  }

  @Test
  fun `resendMfaCode check email`() {
    val user = User.builder().username("user").email("email").build()
    val userToken = user.createToken(TokenType.MFA)
    val userCode = user.createToken(TokenType.MFA_CODE)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    service.resendMfaCode("sometoken", MfaPreferenceType.EMAIL)

    verify(notificationClientApi).sendEmail("emailTemplate", "email", mapOf("firstName" to "user", "code" to userCode.token), null)
  }

  @Test
  fun `resendMfaCode check Text`() {
    val user = User.builder().mobile("07700900321").mobileVerified(true).mfaPreference(MfaPreferenceType.TEXT).build()
    val userToken = user.createToken(TokenType.MFA)
    val userCode = user.createToken(TokenType.MFA_CODE)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    service.resendMfaCode("sometoken", MfaPreferenceType.TEXT)

    verify(notificationClientApi).sendSms("textTemplate", "07700900321", mapOf("mfaCode" to userCode.token), null, null)
  }

  @Test
  fun `Update User Mfa Preference to text`() {
    val user = User.of("user")
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(user))
    service.updateUserMfaPreference(MfaPreferenceType.TEXT, "user")
    assertThat(user.mfaPreference).isEqualTo(MfaPreferenceType.TEXT)
    verify(userService).findUser("user")
  }

  @Test
  fun `Update User Mfa Preference to email`() {
    val user = User.builder().username("user").mfaPreference(MfaPreferenceType.TEXT).build()
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(user))
    service.updateUserMfaPreference(MfaPreferenceType.EMAIL, "user")
    assertThat(user.mfaPreference).isEqualTo(MfaPreferenceType.EMAIL)
    verify(userService).findUser("user")
  }


  @Test
  fun `buildModelAndViewWithMfaResendOptions check view`() {
    whenever(tokenService.getUserFromToken(any(), anyString())).thenReturn(User())
    val modelAndView = service.buildModelAndViewWithMfaResendOptions("token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.viewName).isEqualTo("mfaResend")
  }

  @Test
  fun `buildModelAndViewWithMfaResendOptions check model`() {
    whenever(tokenService.getUserFromToken(any(), anyString())).thenReturn(User())
    val modelAndView = service.buildModelAndViewWithMfaResendOptions("token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.model).containsExactly(entry("token", "token"), entry("mfaPreference", MfaPreferenceType.EMAIL))
  }

  @Test
  fun `buildModelAndViewWithMfaResendOptions verified mobile and email check model`() {
    val user = User.builder().mobile("07700900321").mobileVerified(true).email("bob@digital.justice.gov.uk").verified(true).build()
    whenever(tokenService.getUserFromToken(any(), anyString())).thenReturn(user)
    val modelAndView = service.buildModelAndViewWithMfaResendOptions("token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.model).containsExactly(
        entry("token", "token"),
        entry("mfaPreference", MfaPreferenceType.EMAIL),
        entry("email", "b******@******.gov.uk"),
        entry("mobile", "*******0321"))
  }

  @Test
  fun `buildModelAndViewWithMfaResendOptions verified mobile and not verfied email check model`() {
    val user = User.builder().mobile("07700900321").mobileVerified(true).email("bob@digital.justice.gov.uk").verified(false).build()
    whenever(tokenService.getUserFromToken(any(), anyString())).thenReturn(user)
    val modelAndView = service.buildModelAndViewWithMfaResendOptions("token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.model).containsExactly(
        entry("token", "token"),
        entry("mfaPreference", MfaPreferenceType.EMAIL),
        entry("mobile", "*******0321"))
  }

  @Test
  fun `buildModelAndViewWithMfaResendOptions not verified mobile and verfied email check model`() {
    val user = User.builder().mobile("07700900321").mobileVerified(false).email("bob@digital.justice.gov.uk").verified(true).build()
    whenever(tokenService.getUserFromToken(any(), anyString())).thenReturn(user)
    val modelAndView = service.buildModelAndViewWithMfaResendOptions("token", MfaPreferenceType.EMAIL)
    assertThat(modelAndView.model).containsExactly(
        entry("token", "token"),
        entry("mfaPreference", MfaPreferenceType.EMAIL),
        entry("email", "b******@******.gov.uk"))
  }


}

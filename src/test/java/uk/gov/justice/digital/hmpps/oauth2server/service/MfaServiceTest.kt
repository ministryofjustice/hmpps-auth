package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import uk.gov.service.notify.NotificationClientApi
import java.util.*

class MfaServiceTest {
  private val tokenService: TokenService = mock()
  private val userService: UserService = mock()
  private val notificationClientApi: NotificationClientApi = mock()
  private val request = MockHttpServletRequest()
  private lateinit var service: MfaService

  @Before
  fun setUp() {
    service = MfaService(setOf("12.21.23.24"), setOf("MFA"), "template", tokenService, userService, notificationClientApi)
    request.remoteAddr = "0:0:0:0:0:0:0:1"
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request, null))
  }

  @After
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
  fun `validateMfaCode null`() {
    assertThat(service.validateAndRemoveMfaCode("", null)).get().isEqualTo("missingcode")
  }

  @Test
  fun `validateMfaCode blank`() {
    assertThat(service.validateAndRemoveMfaCode("", "   ")).get().isEqualTo("missingcode")
  }

  @Test
  fun `validateMfaCode token error`() {
    whenever(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("someproblem"))
    assertThat(service.validateAndRemoveMfaCode("", "somecode")).get().isEqualTo("someproblem")
  }

  @Test
  fun `validateMfaCode success`() {
    assertThat(service.validateAndRemoveMfaCode("sometoken", "somecode")).isEmpty
  }

  @Test
  fun `validateMfaCode remove tokens`() {
    assertThat(service.validateAndRemoveMfaCode("sometoken", "somecode")).isEmpty
    verify(tokenService).removeToken(TokenType.MFA, "sometoken")
    verify(tokenService).removeToken(TokenType.MFA_CODE, "somecode")
  }

  @Test
  fun `createTokenAndSendEmail success`() {
    val user = User.of("bob")
    whenever(userService.getOrCreateUser(anyString())).thenReturn(user)
    whenever(tokenService.createToken(eq(TokenType.MFA), anyString())).thenReturn("sometoken")
    whenever(tokenService.createToken(eq(TokenType.MFA_CODE), anyString())).thenReturn("somecode")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    assertThat(service.createTokenAndSendEmail("user")).isEqualTo(Pair("sometoken", "somecode"))
  }

  @Test
  fun `createTokenAndSendEmail check email params`() {
    val user = User.builder().username("bob").person(Person("first", "last")).email("email").build()
    whenever(userService.getOrCreateUser(anyString())).thenReturn(user)
    whenever(tokenService.createToken(eq(TokenType.MFA), anyString())).thenReturn("sometoken")
    whenever(tokenService.createToken(eq(TokenType.MFA_CODE), anyString())).thenReturn("somecode")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    service.createTokenAndSendEmail("user")

    verify(notificationClientApi).sendEmail("template", "email", mapOf("firstName" to "first", "code" to "somecode"), null)
  }

  @Test
  fun `resendMfaCode no code`() {
    val userToken = User.of("user").createToken(TokenType.MFA)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))

    val code = service.resendMfaCode("sometoken")
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

    assertThat(service.resendMfaCode("sometoken")).isEqualTo(userCode.token)
  }

  @Test
  fun `resendMfaCode check email`() {
    val user = User.builder().username("user").email("email").build()
    val userToken = user.createToken(TokenType.MFA)
    val userCode = user.createToken(TokenType.MFA_CODE)
    whenever(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(userToken))
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))

    service.resendMfaCode("sometoken")

    verify(notificationClientApi).sendEmail("template", "email", mapOf("firstName" to "user", "code" to userCode.token), null)
  }

}

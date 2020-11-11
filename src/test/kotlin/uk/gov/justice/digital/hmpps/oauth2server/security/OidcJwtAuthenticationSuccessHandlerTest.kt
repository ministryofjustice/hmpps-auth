package uk.gov.justice.digital.hmpps.oauth2server.security

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.azure.AzureUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.time.Instant
import java.util.ArrayList
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class OidcJwtAuthenticationSuccessHandlerTest {
  private val mockJwtCookieHelper: JwtCookieHelper = mock()
  private val mockJwtAuthenticationHelper: JwtAuthenticationHelper = mock()
  private val cookieRequestCache: CookieRequestCache = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val restTemplate: RestTemplate = mock()
  private val userRetriesService: UserRetriesService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val mockRequest: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val authentication: Authentication = mock()

  private val oidcJwtAuthenticationSuccessHandler = OidcJwtAuthenticationSuccessHandler(
    mockJwtCookieHelper,
    mockJwtAuthenticationHelper,
    cookieRequestCache,
    verifyEmailService,
    restTemplate,
    tokenVerificationEnabled = false,
    userRetriesService,
    telemetryClient,
  )

  @Test
  fun `onAuthenticationSuccess uses GivenName and FamilyName when available`() {
    val oidcToken = OidcIdToken(
      "tokenValue",
      Instant.now(),
      Instant.now().plusSeconds(1000),
      // id claims
      mapOf(
        "sub" to "6C2x9vsoM3Fgi9QmeYZGUT4hdYme3Dw1566tp8yc1vE",
        "given_name" to "Joe",
        "family_name" to "Bloggs",
        "oid" to "d6165ad0-aed3-4146-9ef7-222876b57549",
        "preferred_username" to "Joe.Bloggs@justice.gov.uk",
      )
    )

    whenever(authentication.principal)
      .thenReturn(DefaultOidcUser(listOf(OidcUserAuthority(oidcToken)), oidcToken))
    whenever(authentication.name).thenReturn("Bob")
    oidcJwtAuthenticationSuccessHandler.onAuthenticationSuccess(mockRequest, response, authentication)

    verify(userRetriesService).resetRetriesAndRecordLogin(
      AzureUserPersonDetails(
        ArrayList(),
        true,
        "D6165AD0-AED3-4146-9EF7-222876B57549",
        "Joe",
        "Bloggs",
        "joe.bloggs@justice.gov.uk",
        true,
        accountNonExpired = true,
        accountNonLocked = true
      )
    )
  }

  @Test
  fun `onAuthenticationSuccess uses Name if GivenName and FamilyName not available`() {
    val oidcToken = OidcIdToken(
      "tokenValue",
      Instant.now(),
      Instant.now().plusSeconds(1000),
      // id claims
      mapOf(
        "sub" to "6C2x9vsoM3Fgi9QmeYZGUT4hdYme3Dw1566tp8yc1vE",
        "name" to "Bloggs, Joe",
        "oid" to "d6165ad0-aed3-4146-9ef7-222876b57549",
        "preferred_username" to "joe.bloggs@justice.gov.uk",
      )
    )

    whenever(authentication.principal)
      .thenReturn(DefaultOidcUser(listOf(OidcUserAuthority(oidcToken)), oidcToken))
    whenever(authentication.name).thenReturn("Bob")

    oidcJwtAuthenticationSuccessHandler.onAuthenticationSuccess(mockRequest, response, authentication)

    verify(userRetriesService).resetRetriesAndRecordLogin(
      AzureUserPersonDetails(
        ArrayList(),
        true,
        "D6165AD0-AED3-4146-9EF7-222876B57549",
        "Joe",
        "Bloggs",
        "joe.bloggs@justice.gov.uk",
        true,
        accountNonExpired = true,
        accountNonLocked = true
      )
    )
  }

  @Test
  fun `onAuthenticationSuccess generates authentication success event`() {
    val oidcToken = OidcIdToken(
      "tokenValue",
      Instant.now(),
      Instant.now().plusSeconds(1000),
      // id claims
      mapOf(
        "sub" to "6C2x9vsoM3Fgi9QmeYZGUT4hdYme3Dw1566tp8yc1vE",
        "name" to "Bloggs, Joe",
        "oid" to "d6165ad0-aed3-4146-9ef7-222876b57549",
        "preferred_username" to "joe.bloggs@justice.gov.uk",
      )
    )

    whenever(authentication.principal)
      .thenReturn(DefaultOidcUser(listOf(OidcUserAuthority(oidcToken)), oidcToken))
    whenever(authentication.name).thenReturn("Bob")

    oidcJwtAuthenticationSuccessHandler.onAuthenticationSuccess(mockRequest, response, authentication)

    verify(telemetryClient).trackEvent(
      "AuthenticateSuccess",
      mapOf("username" to "joe.bloggs@justice.gov.uk"),
      null
    )
  }

  @Test
  fun `onAuthenticationSuccess throws exception if principal not DefaultOidcUser`() {
    whenever(authentication.principal)
      .thenReturn("Dummy principal")

    assertThatThrownBy {
      oidcJwtAuthenticationSuccessHandler
        .onAuthenticationSuccess(mockRequest, response, authentication)
    }.isInstanceOf(RuntimeException::class.java)
  }
}

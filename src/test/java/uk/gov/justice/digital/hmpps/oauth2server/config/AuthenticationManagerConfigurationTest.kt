package uk.gov.justice.digital.hmpps.oauth2server.config

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import uk.gov.justice.digital.hmpps.oauth2server.resource.ClearAllSessionsLogoutHandler
import uk.gov.justice.digital.hmpps.oauth2server.resource.LoggingAccessDeniedHandler
import uk.gov.justice.digital.hmpps.oauth2server.resource.RedirectingLogoutSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthAuthenticationProvider
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationProvider
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtCookieAuthenticationFilter
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisAuthenticationProvider
import uk.gov.justice.digital.hmpps.oauth2server.security.OidcJwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserStateAuthenticationFailureHandler
import java.util.*

class AuthenticationManagerConfigurationTest {
  private val nomisUserDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> = mock()
  private val authUserDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> = mock()
  private val deliusUserDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> = mock()
  private val azureUserDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> = mock()
  private val accessDeniedHandler: LoggingAccessDeniedHandler = mock()
  private val logoutSuccessHandler: RedirectingLogoutSuccessHandler = mock()
  private val oidcJwtAuthenticationSuccessHandler: OidcJwtAuthenticationSuccessHandler = mock()
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler = mock()
  private val jwtCookieAuthenticationFilter: JwtCookieAuthenticationFilter = mock()
  private val jwtCookieName: String = "some cookie"
  private val cookieRequestCache: CookieRequestCache = mock()
  private val authAuthenticationProvider: AuthAuthenticationProvider = mock()
  private val nomisAuthenticationProvider: NomisAuthenticationProvider = mock()
  private val deliusAuthenticationProvider: DeliusAuthenticationProvider = mock()
  private val userStateAuthenticationFailureHandle: UserStateAuthenticationFailureHandler = mock()
  private val authenticationManagerBuilder: AuthenticationManagerBuilder = mock()
  private val clearAllSessionsLogoutHandler: ClearAllSessionsLogoutHandler = mock()
  private val clientRegistrationRepository: Optional<InMemoryClientRegistrationRepository> = Optional.empty()
  private var authenticationManagerConfiguration = AuthenticationManagerConfiguration(nomisUserDetailsService, authUserDetailsService,
      deliusUserDetailsService, azureUserDetailsService, accessDeniedHandler, logoutSuccessHandler, oidcJwtAuthenticationSuccessHandler, jwtAuthenticationSuccessHandler, jwtCookieAuthenticationFilter,
      jwtCookieName, cookieRequestCache, authAuthenticationProvider, nomisAuthenticationProvider, deliusAuthenticationProvider, userStateAuthenticationFailureHandle, clearAllSessionsLogoutHandler, clientRegistrationRepository)

  @Test
  fun configure_deliusProviderIsLast() {
    argumentCaptor<LockingAuthenticationProvider>().apply {
      authenticationManagerConfiguration.configure(authenticationManagerBuilder)
      verify(authenticationManagerBuilder, atLeastOnce()).authenticationProvider(capture())
      val providers = allValues.filter { p: AuthenticationProvider? -> p !is PreAuthenticatedAuthenticationProvider }
      assertThat(providers[providers.size - 1]).isEqualTo(deliusAuthenticationProvider)
    }
  }
}

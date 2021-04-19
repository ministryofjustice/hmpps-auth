@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.web.session.HttpSessionEventPublisher
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import uk.gov.justice.digital.hmpps.oauth2server.resource.ClearAllSessionsLogoutHandler
import uk.gov.justice.digital.hmpps.oauth2server.resource.LoggingAccessDeniedHandler
import uk.gov.justice.digital.hmpps.oauth2server.resource.RedirectingLogoutSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthAuthenticationProvider
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationProvider
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtCookieAuthenticationFilter
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisAuthenticationProvider
import uk.gov.justice.digital.hmpps.oauth2server.security.OidcJwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserStateAuthenticationFailureHandler
import java.util.Optional

@Suppress("SpringElInspection")
@Configuration
@Order(4)
class AuthenticationManagerConfiguration(
  @Qualifier("nomisUserDetailsService") private val nomisUserDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>,
  @Qualifier("authUserDetailsService") private val authUserDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>,
  @Qualifier("deliusUserDetailsService") private val deliusUserDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>,
  @Qualifier("azureUserDetailsService") private val azureUserDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>,
  private val accessDeniedHandler: LoggingAccessDeniedHandler,
  private val logoutSuccessHandler: RedirectingLogoutSuccessHandler,
  private val oidcJwtAuthenticationSuccessHandler: OidcJwtAuthenticationSuccessHandler,
  private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler,
  private val jwtCookieAuthenticationFilter: JwtCookieAuthenticationFilter,
  @Value("\${jwt.cookie.name}") private val jwtCookieName: String,
  private val cookieRequestCache: CookieRequestCache,
  private val authAuthenticationProvider: AuthAuthenticationProvider,
  private val nomisAuthenticationProvider: NomisAuthenticationProvider,
  private val deliusAuthenticationProvider: DeliusAuthenticationProvider,
  private val userStateAuthenticationFailureHandler: UserStateAuthenticationFailureHandler,
  private val clearAllSessionsLogoutHandler: ClearAllSessionsLogoutHandler,
  private val clientRegistrationRepository: Optional<InMemoryClientRegistrationRepository>,
) : WebSecurityConfigurerAdapter() {

  override fun configure(http: HttpSecurity) {

    // @formatter:off
    http
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Can't have CSRF protection as requires session
      .and().csrf().disable()
      .authorizeRequests()
      .antMatchers(HttpMethod.GET, "/login").permitAll()
      .antMatchers(HttpMethod.POST, "/login").permitAll()
      .antMatchers("/ui/**").access("isAuthenticated() and @authIpSecurity.check(request)")
      .antMatchers("/api/**").access("isAuthenticated() and @authIpSecurity.check(request)")
      .anyRequest().authenticated()
      .and()
      .formLogin()
      .loginPage("/login")
      .successHandler(jwtAuthenticationSuccessHandler)
      .failureHandler(userStateAuthenticationFailureHandler)
      .permitAll()
      .and()
      .logout()
      .invalidateHttpSession(true)
      .clearAuthentication(true)
      .deleteCookies(jwtCookieName)
      .logoutRequestMatcher(AntPathRequestMatcher("/logout"))
      .addLogoutHandler(clearAllSessionsLogoutHandler)
      .logoutSuccessHandler(logoutSuccessHandler)
      .permitAll()
      .and()
      .exceptionHandling()
      .accessDeniedHandler(accessDeniedHandler)
      .and()
      .addFilterAfter(jwtCookieAuthenticationFilter, BasicAuthenticationFilter::class.java)
      .requestCache().requestCache(cookieRequestCache)

    if (clientRegistrationRepository.isPresent) {
      http.oauth2Login()
        .userInfoEndpoint(Customizer { it.oidcUserService(oidcUserService()) })
        .loginPage("/login")
        .successHandler(oidcJwtAuthenticationSuccessHandler)
        .failureHandler(userStateAuthenticationFailureHandler)
        .permitAll()
    }
    // @formatter:on
  }

  /**
   * Custom User Service for the Azure OIDC integration. Spring expects to get the username from the userinfo endpoint,
   * unfortunately the Azure endpoint doesn't return the field we need - i.e. oid. Therefore the username field is set
   * to sub in the configuration, and modified here once the token and userinfo attributes are merged.
   *
   *
   * Also capitalises the username as other code expects all usernames to be uppercase.
   */
  @Bean
  fun oidcUserService(): OAuth2UserService<OidcUserRequest, OidcUser> {
    val delegate = OidcUserService()
    return OAuth2UserService { userRequest: OidcUserRequest? ->
      // Delegate to the default implementation for loading a user
      val oidcUser = delegate.loadUser(userRequest)

      // Now we have the claims from the id token and the userinfo response combined, we can set the preferred_username field to be the name source
      val idToken = oidcUser.idToken
      val oidcIdToken = OidcIdToken(idToken.tokenValue, idToken.issuedAt, idToken.expiresAt, idToken.claims)
      DefaultOidcUser(oidcUser.authorities, oidcIdToken, oidcUser.userInfo, "oid")
    }
  }

  override fun configure(web: WebSecurity) {
    web
      .ignoring()
      .antMatchers(
        "/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**", "/favicon.ico",
        "/health/**", "/info", "/ping", "/error", "/terms", "/contact", "/change-password",
        "/verify-email-confirm", "/verify-email-secondary-confirm", "/forgot-password", "/reset-password",
        "/set-password", "/reset-password-confirm", "/reset-password-success", "/reset-password-select",
        "/initial-password", "/initial-password-success", "/initial-password-expired", "/mfa-challenge",
        "/verify-email-expired", "/verify-email-secondary-expired", "/verify-email-failure",
        "/mfa-resend", "/h2-console/**", "/v2/api-docs", "/jwt-public-key",
        "/swagger-ui.html", "/swagger-resources", "/swagger-resources/configuration/ui",
        "/swagger-resources/configuration/security", "/.well-known/jwks.json", "/issuer/.well-known/**",
        "/api/services"
      )
  }

  @Bean
  @Throws(Exception::class)
  override fun authenticationManagerBean(): AuthenticationManager = super.authenticationManagerBean()

  /**
   * An assumption is made in DeliusUserService that the delius auth provider is checked last and if Delius is down
   * then we do not check with any further providers.
   */
  public override fun configure(auth: AuthenticationManagerBuilder) {
    auth.authenticationProvider(authAuthenticationProvider)
    auth.authenticationProvider(nomisAuthenticationProvider)
    auth.authenticationProvider(deliusAuthenticationProvider)
    auth.authenticationProvider(preAuthProvider(authUserDetailsService))
    auth.authenticationProvider(preAuthProvider(nomisUserDetailsService))
    auth.authenticationProvider(preAuthProvider(azureUserDetailsService))
    auth.authenticationProvider(preAuthProvider(deliusUserDetailsService))
  }

  private fun preAuthProvider(userDetailsService: AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>): PreAuthenticatedAuthenticationProvider {
    val preAuth = PreAuthenticatedAuthenticationProvider()
    preAuth.setPreAuthenticatedUserDetailsService(userDetailsService)
    return preAuth
  }

  @Bean
  fun httpSessionEventPublisher(): HttpSessionEventPublisher = HttpSessionEventPublisher()

  @Bean
  fun registration(filter: JwtCookieAuthenticationFilter): FilterRegistrationBean<*> {
    // have to explicitly disable the filter otherwise it will be registered with spring as a global filter
    val registration = FilterRegistrationBean(filter)
    registration.isEnabled = false
    return registration
  }

  @Bean
  fun redirectResolver(@Value("\${application.authentication.match-subdomains}") matchSubdomains: Boolean): RedirectResolver {
    val redirectResolver = DefaultRedirectResolver()
    redirectResolver.setMatchSubdomains(matchSubdomains)
    return redirectResolver
  }
}

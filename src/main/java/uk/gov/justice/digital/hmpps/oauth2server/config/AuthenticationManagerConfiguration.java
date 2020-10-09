package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver;
import org.springframework.security.oauth2.provider.endpoint.RedirectResolver;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.digital.hmpps.oauth2server.resource.ClearAllSessionsLogoutHandler;
import uk.gov.justice.digital.hmpps.oauth2server.resource.LoggingAccessDeniedHandler;
import uk.gov.justice.digital.hmpps.oauth2server.resource.RedirectingLogoutSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtCookieAuthenticationFilter;
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.OidcJwtAuthenticationSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserStateAuthenticationFailureHandler;

import java.util.Optional;

@Configuration
@Order(4)
public class AuthenticationManagerConfiguration extends WebSecurityConfigurerAdapter {

    private final LoggingAccessDeniedHandler accessDeniedHandler;
    private final RedirectingLogoutSuccessHandler logoutSuccessHandler;
    private final OidcJwtAuthenticationSuccessHandler oidcJwtAuthenticationSuccessHandler;
    private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;
    private final String jwtCookieName;
    private final CookieRequestCache cookieRequestCache;
    private final AuthAuthenticationProvider authAuthenticationProvider;
    private final NomisAuthenticationProvider nomisAuthenticationProvider;
    private final DeliusAuthenticationProvider deliusAuthenticationProvider;
    private final UserStateAuthenticationFailureHandler userStateAuthenticationFailureHandler;
    private final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> nomisUserDetailsService;
    private final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> authUserDetailsService;
    private final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> deliusUserDetailsService;
    private final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> azureUserDetailsService;
    private final ClearAllSessionsLogoutHandler clearAllSessionsLogoutHandler;
    private final Optional<InMemoryClientRegistrationRepository> clientRegistrationRepository;

    @Autowired
    public AuthenticationManagerConfiguration(@Qualifier("nomisUserDetailsService") final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> nomisUserDetailsService,
                                              @Qualifier("authUserDetailsService") final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> authUserDetailsService,
                                              @Qualifier("deliusUserDetailsService") final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> deliusUserDetailsService,
                                              @Qualifier("azureUserDetailsService") final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> azureUserDetailsService,
                                              final LoggingAccessDeniedHandler accessDeniedHandler,
                                              final RedirectingLogoutSuccessHandler logoutSuccessHandler,
                                              final OidcJwtAuthenticationSuccessHandler oidcJwtAuthenticationSuccessHandler,
                                              final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler,
                                              final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter,
                                              @Value("${jwt.cookie.name}") final String jwtCookieName,
                                              final CookieRequestCache cookieRequestCache,
                                              final AuthAuthenticationProvider authAuthenticationProvider,
                                              final NomisAuthenticationProvider nomisAuthenticationProvider,
                                              final DeliusAuthenticationProvider deliusAuthenticationProvider,
                                              final UserStateAuthenticationFailureHandler userStateAuthenticationFailureHandler,
                                              final ClearAllSessionsLogoutHandler clearAllSessionsLogoutHandler,
                                              final Optional<InMemoryClientRegistrationRepository> clientRegistrationRepository) {
        this.nomisUserDetailsService = nomisUserDetailsService;
        this.authUserDetailsService = authUserDetailsService;
        this.azureUserDetailsService = azureUserDetailsService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.oidcJwtAuthenticationSuccessHandler = oidcJwtAuthenticationSuccessHandler;
        this.jwtAuthenticationSuccessHandler = jwtAuthenticationSuccessHandler;
        this.jwtCookieAuthenticationFilter = jwtCookieAuthenticationFilter;
        this.jwtCookieName = jwtCookieName;
        this.cookieRequestCache = cookieRequestCache;
        this.authAuthenticationProvider = authAuthenticationProvider;
        this.nomisAuthenticationProvider = nomisAuthenticationProvider;
        this.deliusAuthenticationProvider = deliusAuthenticationProvider;
        this.userStateAuthenticationFailureHandler = userStateAuthenticationFailureHandler;
        this.deliusUserDetailsService = deliusUserDetailsService;
        this.clearAllSessionsLogoutHandler = clearAllSessionsLogoutHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @SuppressWarnings("SpringElInspection")
    @Override
    protected void configure(final HttpSecurity http) throws Exception {

        // @formatter:off
        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                // Can't have CSRF protection as requires session
                .and().csrf().disable()

                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/login").permitAll()
                .antMatchers(HttpMethod.POST, "/login").permitAll()
                .antMatchers("/ui/**").access("isAuthenticated() and @authIpSecurity.check(request)")
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
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .addLogoutHandler(clearAllSessionsLogoutHandler)
                .logoutSuccessHandler(logoutSuccessHandler)
                .permitAll()

                .and()
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)

                .and()
                .addFilterAfter(jwtCookieAuthenticationFilter, BasicAuthenticationFilter.class)

                .requestCache().requestCache(cookieRequestCache);

        if (clientRegistrationRepository.isPresent()) {
            http.oauth2Login()
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(this.oidcUserService()))
                    .loginPage("/login")
                    .successHandler(oidcJwtAuthenticationSuccessHandler)
                    .failureHandler(userStateAuthenticationFailureHandler)
                    .permitAll();
        }
        // @formatter:on
    }

    /**
     * Custom User Service for the Azure OIDC integration. Spring expects to get the username from the userinfo endpoint,
     * unfortunately the Azure endpoint doesn't return the field we need - i.e. oid. Therefore the username field is set
     * to sub in the configuration, and modified here once the token and userinfo attributes are merged.
     * <p>
     * Also capitalises the username as other code expects all usernames to be uppercase.
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final var delegate = new OidcUserService();

        return (userRequest) -> {
            // Delegate to the default implementation for loading a user
            final var oidcUser = delegate.loadUser(userRequest);

            // Now we have the claims from the id token and the userinfo response combined, we can set the preferred_username field to be the name source
            final var idToken = oidcUser.getIdToken();

            final var oidcIdToken = new OidcIdToken(idToken.getTokenValue(), idToken.getIssuedAt(), idToken.getExpiresAt(), idToken.getClaims());

            return new DefaultOidcUser(oidcUser.getAuthorities(), oidcIdToken, oidcUser.getUserInfo(), "oid");
        };
    }

    @Override
    public void configure(final WebSecurity web) {
        web
                .ignoring()
                .antMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**", "/favicon.ico",
                        "/health/**", "/info", "/ping", "/error", "/terms", "/contact", "/change-password",
                        "/verify-email-confirm", "/verify-email-secondary-confirm", "/forgot-password", "/reset-password",
                        "/set-password", "/reset-password-confirm", "/reset-password-success", "/reset-password-select",
                        "/initial-password", "/initial-password-success", "/initial-password-expired", "/mfa-challenge",
                        "/verify-email-expired", "/verify-email-secondary-expired", "/verify-email-failure",
                        "/mfa-resend", "/h2-console/**", "/v2/api-docs", "/jwt-public-key",
                        "/swagger-ui.html", "/swagger-resources", "/swagger-resources/configuration/ui",
                        "/swagger-resources/configuration/security", "/.well-known/jwks.json", "/issuer/.well-known/**");
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * An assumption is made in DeliusUserService that the delius auth provider is checked last and if Delius is down
     * then we do not check with any further providers.
     */
    @Override
    protected void configure(final AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authAuthenticationProvider);
        auth.authenticationProvider(nomisAuthenticationProvider);
        auth.authenticationProvider(deliusAuthenticationProvider);
        auth.authenticationProvider(preAuthProvider(authUserDetailsService));
        auth.authenticationProvider(preAuthProvider(nomisUserDetailsService));
        auth.authenticationProvider(preAuthProvider(azureUserDetailsService));
        auth.authenticationProvider(preAuthProvider(deliusUserDetailsService));
    }

    private PreAuthenticatedAuthenticationProvider preAuthProvider(final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService) {
        final var preAuth = new PreAuthenticatedAuthenticationProvider();
        preAuth.setPreAuthenticatedUserDetailsService(userDetailsService);
        return preAuth;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public FilterRegistrationBean registration(final JwtCookieAuthenticationFilter filter) {
        // have to explicitly disable the filter otherwise it will be registered with spring as a global filter
        final var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @SuppressWarnings("deprecation")
    @Bean
    public RedirectResolver redirectResolver(@Value("${application.authentication.match-subdomains}") final boolean matchSubdomains) {
        final var redirectResolver = new DefaultRedirectResolver();
        redirectResolver.setMatchSubdomains(matchSubdomains);
        return redirectResolver;
    }

    @Bean
    @Conditional(ClientsConfiguredCondition.class)
    WebClient webClient(final ClientRegistrationRepository clientRegistrationRepository,
                        final OAuth2AuthorizedClientRepository authorizedClientRepository) {
        final var oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrationRepository,
                        authorizedClientRepository);
        oauth2.setDefaultOAuth2AuthorizedClient(true);
        return WebClient.builder().apply(oauth2.oauth2Configuration()).build();
    }
}

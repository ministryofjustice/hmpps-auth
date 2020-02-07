package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import uk.gov.justice.digital.hmpps.oauth2server.resource.LoggingAccessDeniedHandler;
import uk.gov.justice.digital.hmpps.oauth2server.resource.RedirectingLogoutSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthenticationManagerConfigurationTest {

    @Mock private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> nomisUserDetailsService;
    @Mock private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> authUserDetailsService;
    @Mock private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> deliusUserDetailsService;
    @Mock private LoggingAccessDeniedHandler accessDeniedHandler;
    @Mock private RedirectingLogoutSuccessHandler logoutSuccessHandler;
    @Mock private JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    @Mock private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;
    @Mock private String jwtCookieName;
    @Mock private CookieRequestCache cookieRequestCache;
    @Mock private AuthAuthenticationProvider authAuthenticationProvider;
    @Mock private NomisAuthenticationProvider nomisAuthenticationProvider;
    @Mock private DeliusAuthenticationProvider deliusAuthenticationProvider;
    @Mock private UserStateAuthenticationFailureHandler userStateAuthenticationFailureHandle;

    private AuthenticationManagerConfiguration authenticationManagerConfiguration;

    @BeforeEach
    void setup() {
        authenticationManagerConfiguration = new AuthenticationManagerConfiguration(nomisUserDetailsService, authUserDetailsService,
                deliusUserDetailsService, accessDeniedHandler, logoutSuccessHandler, jwtAuthenticationSuccessHandler, jwtCookieAuthenticationFilter,
                jwtCookieName, cookieRequestCache, authAuthenticationProvider, nomisAuthenticationProvider, deliusAuthenticationProvider,
                userStateAuthenticationFailureHandle);
    }


    @Test
    void configure_deliusProviderIsLast() {
        AuthenticationManagerBuilder authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);

        authenticationManagerConfiguration.configure(authenticationManagerBuilder);

        ArgumentCaptor<AuthenticationProvider> captor = ArgumentCaptor.forClass(LockingAuthenticationProvider.class);
        verify(authenticationManagerBuilder, atLeastOnce()).authenticationProvider(captor.capture());
        List<AuthenticationProvider> providers = captor.getAllValues().stream().filter(p -> !(p instanceof PreAuthenticatedAuthenticationProvider)).collect(Collectors.toList());
        assertThat(providers.get(providers.size()-1)).isEqualTo(deliusAuthenticationProvider);
    }

}

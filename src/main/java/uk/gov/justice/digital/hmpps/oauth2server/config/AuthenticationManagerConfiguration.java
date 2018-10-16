package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import uk.gov.justice.digital.hmpps.oauth2server.resource.LoggingAccessDeniedHandler;
import uk.gov.justice.digital.hmpps.oauth2server.resource.RedirectingLogoutSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.ApiAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsServiceImpl;

@Configuration
@Order(4)
public class AuthenticationManagerConfiguration extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final LoggingAccessDeniedHandler accessDeniedHandler;
    private final RedirectingLogoutSuccessHandler logoutSuccessHandler;

    @Autowired
    public AuthenticationManagerConfiguration(final UserDetailsService userDetailsService,
                                              final LoggingAccessDeniedHandler accessDeniedHandler,
                                              final RedirectingLogoutSuccessHandler logoutSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .authorizeRequests()
                .antMatchers("/css/**", "/img/**","/font/**", "/webjars/**", "/favicon.ico",
                        "/health", "/info",
                        "/h2-console/**", "/v2/api-docs", "/configuration/ui", "/configuration/security",
                        "/swagger-ui.html", "/swagger-resources/configuration/ui",
                        "/swagger-resources/configuration/security").permitAll()

                .anyRequest().authenticated()

            .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()

            .and()
                .logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessHandler(logoutSuccessHandler)
                .logoutSuccessUrl("/login?logout")
                .permitAll()

            .and()
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler);

        // @formatter:on
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider());
        auth.authenticationProvider(preAuthProvider());
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new ApiAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthProvider() {
        PreAuthenticatedAuthenticationProvider preAuth = new PreAuthenticatedAuthenticationProvider();
        preAuth.setPreAuthenticatedUserDetailsService((UserDetailsServiceImpl) userDetailsService);
        return preAuth;
    }


}

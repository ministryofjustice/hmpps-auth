package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import uk.gov.justice.digital.hmpps.oauth2server.resource.LoggingAccessDeniedHandler;
import uk.gov.justice.digital.hmpps.oauth2server.resource.RedirectingLogoutSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.ApiAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsServiceImpl;

@Configuration
@Order(1)
public class AuthenticationManagerConfiguration extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final LoggingAccessDeniedHandler accessDeniedHandler;
    private final RedirectingLogoutSuccessHandler logoutSuccessHandler;

    private final boolean requireSsl;

    @Autowired
    public AuthenticationManagerConfiguration(final UserDetailsService userDetailsService,
                                              final LoggingAccessDeniedHandler accessDeniedHandler,
                                              @Value("${application.requires.ssl:false}") final boolean requireSsl,
                                              final RedirectingLogoutSuccessHandler logoutSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.requireSsl = requireSsl;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .authorizeRequests()
                .antMatchers("/css/**", "/img/**","/font/**", "/webjars/**", "/favicon.ico").permitAll()

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
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
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

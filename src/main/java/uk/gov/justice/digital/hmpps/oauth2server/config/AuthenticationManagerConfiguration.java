package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.ApiAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class AuthenticationManagerConfiguration extends WebSecurityConfigurerAdapter {


    private final UserDetailsService userDetailsService;

    public AuthenticationManagerConfiguration(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder delegatingPasswordEncoder() {
        PasswordEncoder defaultEncoder = NoOpPasswordEncoder.getInstance();
//        Map<String, PasswordEncoder> encoders = new HashMap<>();
//        encoders.put("bcrypt", new BCryptPasswordEncoder());
//        encoders.put("scrypt", new SCryptPasswordEncoder());
//
//        DelegatingPasswordEncoder passworEncoder = new DelegatingPasswordEncoder(
//                "bcrypt", encoders);
//        passworEncoder.setDefaultPasswordEncoderForMatches(defaultEncoder);

        return defaultEncoder;
    }


    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable().antMatcher("/**")
                .cors().disable().antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/h2-console/**", "/v2/api-docs", "/configuration/ui", "/swagger-resources", "/configuration/security",
                        "/swagger-ui.html", "/webjars/**", "/swagger-resources/configuration/ui", "/swagger-ui.html",
                        "/swagger-resources/configuration/security", "/health", "/info").permitAll()
                .anyRequest()
                .authenticated();

        http.headers().frameOptions().disable();

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

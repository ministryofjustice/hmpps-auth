package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.ApiAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsServiceImpl;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

@Configuration
public class AuthenticationManagerConfiguration extends GlobalAuthenticationConfigurerAdapter {

    @Autowired
    private UserService userService;


    @Override
    public void init(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider()).authenticationProvider(preAuthProvider());
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new ApiAuthenticationProvider();
        provider.setUserDetailsService(new UserDetailsServiceImpl(userService));
        return provider;
    }

    @Bean
    public PreAuthenticatedAuthenticationProvider preAuthProvider() {
        PreAuthenticatedAuthenticationProvider preAuth = new PreAuthenticatedAuthenticationProvider();
        preAuth.setPreAuthenticatedUserDetailsService(new UserDetailsServiceImpl(userService));
        return preAuth;
    }


}

package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Order(2)
public class ResourceConfiguration extends WebSecurityConfigurerAdapter {


    @Override
    protected void configure(HttpSecurity http) throws Exception { // @formatter:off
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
    } // @formatter:on


}

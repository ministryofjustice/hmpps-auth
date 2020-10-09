package uk.gov.justice.digital.hmpps.oauth2server.config;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

@SuppressWarnings("deprecation")
@Configuration
@Order(1)
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@EnableResourceServer
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ResourceConfiguration extends ResourceServerConfigurerAdapter {

    private final ResourceServerTokenServices tokenServices;

    @Override
    public void configure(final HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .antMatcher("/api/**")
                .authorizeRequests()
                .anyRequest().authenticated();
        // @formatter:on
    }

    @Override
    public void configure(final ResourceServerSecurityConfigurer config) {
        config.tokenServices(tokenServices);
    }
}

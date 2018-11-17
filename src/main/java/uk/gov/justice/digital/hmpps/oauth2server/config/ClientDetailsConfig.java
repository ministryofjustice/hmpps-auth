package uk.gov.justice.digital.hmpps.oauth2server.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ClientDetailsConfig {
    private final List<OauthClientConfig> oauthClientConfig;
    private final Flyway flyway;
    private final PasswordEncoder passwordEncoder;

    public ClientDetailsConfig(ClientConfigExtractor clientConfigExtractor,
                               @Value("${oauth.client.data}") String clientData,
                               @Qualifier("authFlyway") Flyway flyway,
                               PasswordEncoder passwordEncoder) {
        this.oauthClientConfig = clientConfigExtractor.getClientConfigurations(clientData);
        this.passwordEncoder = passwordEncoder;
        this.flyway = flyway;
    }

    @Bean
    @Primary
    public JdbcClientDetailsService jdbcClientDetailsService() {

        JdbcClientDetailsService jdbcClientDetailsService = new JdbcClientDetailsService(flyway.getConfiguration().getDataSource());
        jdbcClientDetailsService.setPasswordEncoder(passwordEncoder);

        if (oauthClientConfig != null && jdbcClientDetailsService.listClientDetails().size() == 0) {
            for (OauthClientConfig client : oauthClientConfig) {
                log.info("Initialising OAUTH2 Client ID {}", client.getClientId());

                BaseClientDetails cd = new BaseClientDetails();
                cd.setClientId(client.getClientId());
                cd.setClientSecret(client.getClientSecret());

                if (client.getAccessTokenValidity() != null) {
                    cd.setAccessTokenValiditySeconds(client.getAccessTokenValidity());
                }
                if (client.getRefreshTokenValidity() != null) {
                    cd.setRefreshTokenValiditySeconds(client.getRefreshTokenValidity());
                }
                if (client.getScope() != null) {
                    cd.setScope(client.getScope());
                }
                if (client.getAutoApprove() != null) {
                    cd.setAutoApproveScopes(client.getAutoApprove());
                }
                if (client.getAuthorities() != null) {
                    cd.setAuthorities(client.getAuthorities().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
                }
                if (client.getAuthorizedGrantTypes() != null) {
                    cd.setAuthorizedGrantTypes(client.getAuthorizedGrantTypes());
                }
                if (client.getWebServerRedirectUri() != null) {
                    cd.setRegisteredRedirectUri(client.getWebServerRedirectUri());
                }

                jdbcClientDetailsService.addClientDetails(cd);
            }
        }

        return jdbcClientDetailsService;
    }

}

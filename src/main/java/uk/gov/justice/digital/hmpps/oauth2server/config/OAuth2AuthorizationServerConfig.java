package uk.gov.justice.digital.hmpps.oauth2server.config;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.builders.ClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableAuthorizationServer
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@Slf4j
public class OAuth2AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    private static final int HOUR_IN_SECS = 60 * 60;
    private final Resource privateKeyPair;
    private final List<OauthClientConfig> oauthClientConfig;
    private final String keystorePassword;
    private final String keystoreAlias;

    private final ClientDetailsService clientDetailsService;

    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public OAuth2AuthorizationServerConfig(@Lazy AuthenticationManager authenticationManager,
                                           @Lazy ClientDetailsService clientDetailsService,
                                           UserDetailsService userDetailsService,
                                           PasswordEncoder passwordEncoder,
                                           @Value("${jwt.signing.key.pair}") String privateKeyPair,
                                           @Value("${jwt.keystore.password}") String keystorePassword,
                                           @Value("${jwt.keystore.alias:elite2api}") String keystoreAlias,
                                           @Value("${oauth.client.data}") String clientData,
                                           ClientConfigExtractor clientConfigExtractor) {

        this.privateKeyPair = new ByteArrayResource(Base64.decodeBase64(privateKeyPair));
        this.keystorePassword = keystorePassword;
        this.keystoreAlias = keystoreAlias;
        this.oauthClientConfig = clientConfigExtractor.getClientConfigurations(clientData);
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
        this.clientDetailsService = clientDetailsService;
        this.passwordEncoder = passwordEncoder;
    }


    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(privateKeyPair, keystorePassword.toCharArray());
        converter.setKeyPair(keyStoreKeyFactory.getKeyPair(keystoreAlias));
        return converter;
    }

    @Override
    public void configure(final AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer.tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()");
    }

    @Bean
    public TokenEnhancer jwtTokenEnhancer() {
        return new JWTTokenEnhancer();
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .tokenStore(tokenStore())
                .accessTokenConverter(accessTokenConverter())
                .tokenEnhancer(tokenEnhancerChain())
                .authenticationManager(authenticationManager)
                .userDetailsService(userDetailsService)
                .tokenServices(tokenServices());
    }



    @Bean
    public TokenEnhancerChain tokenEnhancerChain() {
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(jwtTokenEnhancer(), accessTokenConverter()));
        return tokenEnhancerChain;
    }

    @Bean
    @Primary
    public DefaultTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenEnhancer(tokenEnhancerChain());
        defaultTokenServices.setTokenStore(tokenStore());
        defaultTokenServices.setReuseRefreshToken(false);  // change to true once refresh period increased.
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setAccessTokenValiditySeconds(HOUR_IN_SECS); // default 1 hours
        defaultTokenServices.setRefreshTokenValiditySeconds(HOUR_IN_SECS * 12); // default 12 hours
        defaultTokenServices.setClientDetailsService(clientDetailsService);
        defaultTokenServices.setAuthenticationManager(authenticationManager);
        return defaultTokenServices;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

        if (oauthClientConfig != null) {
            ClientDetailsServiceBuilder<InMemoryClientDetailsServiceBuilder>.ClientBuilder clientBuilder = null;
            for (OauthClientConfig client : oauthClientConfig) {
                if (clientBuilder == null) {
                    clientBuilder = clients.inMemory().withClient(client.getClientId());
                } else {
                    clientBuilder = clientBuilder.and().withClient(client.getClientId());
                }
                log.info("Initialising OAUTH2 Client ID {}", client.getClientId());
                clientBuilder = clientBuilder
                        .secret(passwordEncoder.encode(client.getClientSecret()))
                        .accessTokenValiditySeconds(client.getAccessTokenValidity())
                        .refreshTokenValiditySeconds(client.getRefreshTokenValidity())
                        .autoApprove(client.isAutoApproved());
                if (client.getScope() != null) {
                    clientBuilder = clientBuilder.scopes(toArray(client.getScope()));
                }
                if (client.getAutoApprove() != null) {
                    clientBuilder = clientBuilder.autoApprove(toArray(client.getAutoApprove()));
                }
                if (client.getAuthorities() != null) {
                    clientBuilder = clientBuilder.authorities(toArray(client.getAuthorities()));
                }
                if (client.getAuthorizedGrantTypes() != null) {
                    clientBuilder = clientBuilder.authorizedGrantTypes(toArray(client.getAuthorizedGrantTypes()));
                }
                if (client.getWebServerRedirectUri() != null) {
                    clientBuilder = clientBuilder.redirectUris(toArray(client.getWebServerRedirectUri()));
                }
            }
        }
    }

    private String[] toArray(List<String> array) {
        if (array != null) {
            return array.toArray(new String[0]);
        }
        return null;
    }

}

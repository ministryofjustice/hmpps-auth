package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JWTTokenEnhancerTest {
    @Mock
    private OAuth2Authentication authentication;

    @Test
    public void testEnhance_HasUserToken() {
        final OAuth2AccessToken token = new DefaultOAuth2AccessToken("value");
        when(authentication.isClientOnly()).thenReturn(false);
        when(authentication.getUserAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(new UserEmail("user"), "pass"));

        new JWTTokenEnhancer().enhance(token, authentication);

        assertThat(token.getAdditionalInformation()).containsOnly(entry("user_name", "user"), entry("auth_source", "auth"));
    }

    @Test
    public void testEnhance_MissingAuthSource() {
        final OAuth2AccessToken token = new DefaultOAuth2AccessToken("value");
        when(authentication.isClientOnly()).thenReturn(false);
        when(authentication.getUserAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(new UserDetailsImpl("user", null, Collections.emptyList(), null), "pass"));

        new JWTTokenEnhancer().enhance(token, authentication);

        assertThat(token.getAdditionalInformation()).containsOnly(entry("user_name", "user"), entry("auth_source", "none"));
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class JWTTokenEnhancerTest {
    @Mock
    private OAuth2Authentication authentication;

    @Test
    void testEnhance_HasUserToken() {
        final OAuth2AccessToken token = new DefaultOAuth2AccessToken("value");
        when(authentication.isClientOnly()).thenReturn(false);
        final var uuid = UUID.randomUUID();
        final var user = User.builder().id(uuid).username("user").source(AuthSource.auth).build();
        when(authentication.getUserAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(user, "pass"));

        new JWTTokenEnhancer().enhance(token, authentication);

        assertThat(token.getAdditionalInformation()).containsOnly(
                entry("user_name", "user"),
                entry("auth_source", "auth"),
                entry("user_id", uuid.toString()));
    }

    @Test
    void testEnhance_MissingAuthSource() {
        final OAuth2AccessToken token = new DefaultOAuth2AccessToken("value");
        when(authentication.isClientOnly()).thenReturn(false);
        when(authentication.getUserAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(new UserDetailsImpl("user", null, Collections.emptyList(), null, "userID"), "pass"));

        new JWTTokenEnhancer().enhance(token, authentication);

        assertThat(token.getAdditionalInformation()).containsOnly(
                entry("user_name", "user"),
                entry("auth_source", "none"),
                entry("user_id", "userID"));
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
public class AuthAuthenticationProviderIntTest {
    @Autowired
    private AuthAuthenticationProvider provider;

    @Test
    void authenticate_AuthUserSuccessWithAuthorities() {
        final var auth = provider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_ADM", "password123456"));
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsOnly("ROLE_OAUTH_ADMIN", "ROLE_MAINTAIN_ACCESS_ROLES", "ROLE_MAINTAIN_OAUTH_USERS");
    }

    @Test
    void authenticate_NullUsername() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken(null, "password"))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    void authenticate_MissingUsername() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("      ", "password"))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    void authenticate_MissingPassword() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "   "))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    void authenticate_AuthUserLockAfterThreeFailures() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_TEST", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_TEST", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_TEST", "wrong"))
        ).isInstanceOf(LockedException.class);
    }

    @Test
    void authenticate_AuthUserResetAfterSuccess() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        // success in middle should cause reset of count
        final var auth = provider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_USER", "password123456"));
        assertThat(auth).isNotNull();

        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_ExpiredUserWrongPassword() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("EXPIRED_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);
    }
}

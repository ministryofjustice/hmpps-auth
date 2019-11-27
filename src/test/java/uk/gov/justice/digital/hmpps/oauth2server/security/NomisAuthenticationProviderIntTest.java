package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
public class NomisAuthenticationProviderIntTest {
    @Autowired
    private NomisAuthenticationProvider provider;

    @Test
    public void authenticate_Success() {
        final var auth = provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "password"));
        assertThat(auth).isNotNull();
    }

    @Test
    public void authenticate_SuccessWithAuthorities() {
        final var auth = provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER_ADM", "password123456"));
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsOnly("ROLE_OAUTH_ADMIN", "ROLE_MAINTAIN_ACCESS_ROLES", "ROLE_KW_MIGRATION", "ROLE_MAINTAIN_OAUTH_USERS");
    }

    @Test
    public void authenticate_NullUsername() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken(null, "password"))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    public void authenticate_MissingUsername() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("      ", "password"))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    public void authenticate_MissingPassword() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "   "))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    public void authenticate_LockAfterThreeFailures() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("CA_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("CA_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("CA_USER", "wrong"))
        ).isInstanceOf(LockedException.class);
    }

    @Test
    public void authenticate_ResetAfterSuccess() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        // success in middle should cause reset of count
        final var auth = provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "password"));
        assertThat(auth).isNotNull();

        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void authenticate_ExpiredUserWrongPassword() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("EXPIRED_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void authenticate_ExpiredUser() {
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("EXPIRED_USER", "password123456"))
        ).isInstanceOf(CredentialsExpiredException.class);
    }
}

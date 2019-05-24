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
public class LockingAuthenticationProviderIntTest {
    @Autowired
    private LockingAuthenticationProvider nomisLockingAuthenticationProvider;

    @Test
    public void authenticate_Success() {
        final var auth = nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "password"));
        assertThat(auth).isNotNull();
    }

    @Test
    public void authenticate_SuccessWithAuthorities() {
        final var auth = nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER_ADM", "password123456"));
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsOnly("ROLE_OAUTH_ADMIN", "ROLE_MAINTAIN_ACCESS_ROLES", "ROLE_KW_MIGRATION", "ROLE_MAINTAIN_OAUTH_USERS");
    }

    @Test
    public void authenticate_AuthUserSuccessWithAuthorities() {
        final var auth = nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_ADM", "password123456"));
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsOnly("ROLE_OAUTH_ADMIN", "ROLE_MAINTAIN_ACCESS_ROLES");
    }

    @Test
    public void authenticate_NullUsername() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(null, "password"))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    public void authenticate_MissingUsername() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("      ", "password"))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    public void authenticate_MissingPassword() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "   "))
        ).isInstanceOf(MissingCredentialsException.class);
    }

    @Test
    public void authenticate_LockAfterThreeFailures() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("CA_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("CA_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("CA_USER", "wrong"))
        ).isInstanceOf(LockedException.class);
    }

    @Test
    public void authenticate_AuthUserLockAfterThreeFailures() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_TEST", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_TEST", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_TEST", "wrong"))
        ).isInstanceOf(LockedException.class);
    }

    @Test
    public void authenticate_ResetAfterSuccess() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        // success in middle should cause reset of count
        final var auth = nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "password"));
        assertThat(auth).isNotNull();

        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void authenticate_AuthUserResetAfterSuccess() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);

        // success in middle should cause reset of count
        final var auth = nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_USER", "password123456"));
        assertThat(auth).isNotNull();

        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("AUTH_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void authenticate_ExpiredUserWrongPassword() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("EXPIRED_USER", "wrong"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    public void authenticate_ExpiredUser() {
        assertThatThrownBy(() ->
                nomisLockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("EXPIRED_USER", "password123456"))
        ).isInstanceOf(CredentialsExpiredException.class);
    }
}

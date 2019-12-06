package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DeliusAuthenticationProviderTest {
    @Mock
    private DeliusUserService deliusUserService;
    @Mock
    private UserRetriesService userRetriesService;
    @Mock
    private TelemetryClient telemetryClient;

    private DeliusAuthenticationProvider provider;

    @Before
    public void setUp() {
        final var deliusUserDetailsService = new DeliusUserDetailsService(deliusUserService);
        provider = new DeliusAuthenticationProvider(deliusUserService, deliusUserDetailsService, userRetriesService, telemetryClient, 3);
    }

    @Test
    public void authenticate_Success() {
        when(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(
                Optional.of(new DeliusUserPersonDetails("Smith", "Delius", "bob", true, "a@b.com", Set.of())));
        when(deliusUserService.authenticateUser(anyString(), anyString())).thenReturn(Boolean.TRUE);
        final var auth = provider.authenticate(new UsernamePasswordAuthenticationToken("DELIUS_USER", "password"));
        assertThat(auth).isNotNull();
    }

    @Test
    public void authenticate_SuccessWithAuthorities() {
        when(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(Optional.of(
                new DeliusUserPersonDetails("Smith", "Delius", "bob", true, "a@b.com", List.of(new SimpleGrantedAuthority("ROLE_BOB")))));
        when(deliusUserService.authenticateUser(anyString(), anyString())).thenReturn(Boolean.TRUE);
        final var auth = provider.authenticate(new UsernamePasswordAuthenticationToken("ITAG_USER_ADM", "password123456"));
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsOnly("ROLE_PROBATION", "ROLE_BOB");
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
        when(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(
                Optional.of(new DeliusUserPersonDetails("Smith", "Delius", "bob", true, "a@b.com", Set.of())));
        when(userRetriesService.incrementRetries(anyString())).thenReturn(4);
        assertThatThrownBy(() ->
                provider.authenticate(new UsernamePasswordAuthenticationToken("CA_USER", "wrong"))
        ).isInstanceOf(LockedException.class);
    }

    @Test
    public void authenticate_ResetAfterSuccess() {
        final var deliusUser = new DeliusUserPersonDetails("Smith", "Delius", "bob", true, "a@b.com", Set.of());
        when(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(Optional.of(deliusUser));
        when(deliusUserService.authenticateUser(anyString(), anyString())).thenReturn(Boolean.TRUE);

        provider.authenticate(new UsernamePasswordAuthenticationToken("DELIUS_USER", "password"));

        verify(userRetriesService).resetRetriesAndRecordLogin(deliusUser);
    }
}

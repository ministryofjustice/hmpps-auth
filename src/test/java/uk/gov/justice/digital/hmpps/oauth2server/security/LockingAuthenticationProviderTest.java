package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LockingAuthenticationProviderTest {
    @Mock
    private UserRetriesService userRetriesService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private TelemetryClient telemetryClient;

    private LockingAuthenticationProvider lockingAuthenticationProvider;

    @Before
    public void setUp() {
        lockingAuthenticationProvider = new LockingAuthenticationProvider(userDetailsService, userRetriesService, telemetryClient, 3);
    }

    @Test
    public void authenticate_nomisUser() {
        // test that oracle passwords are authenticated okay
        final var password = "S:39BA463D55E5C8936A6798CC37B1347BA8BEC37B6407397EB769BC356F0C";

        final var userDetails = new UserDetailsImpl("user", "name", password,
                true, true, true, true, Collections.emptyList());

        when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails);

        lockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("user", "somepass1"));
    }

    @Test
    public void authenticate_authUser() {
        final var password = "{bcrypt}" + new BCryptPasswordEncoder().encode("some_pass");

        final var userDetails = new UserDetailsImpl("user", "name", password,
                true, true, true, true, Collections.emptyList());

        when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails);

        lockingAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("user", "some_pass"));
    }
}

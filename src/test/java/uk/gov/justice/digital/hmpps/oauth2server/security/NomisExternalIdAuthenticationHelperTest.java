package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NomisExternalIdAuthenticationHelperTest {
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private NomisUserService userService;

    private ExternalIdAuthenticationHelper helper;

    @Before
    public void setUp() {
        helper = new ExternalIdAuthenticationHelper(userService, userDetailsService);
    }

    @Test
    public void getUserDetails_notFound() {
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(null);

        final var details = helper.getUserDetails(Map.of("username", "bobuser"));

        assertThat(details).isNull();
    }
}

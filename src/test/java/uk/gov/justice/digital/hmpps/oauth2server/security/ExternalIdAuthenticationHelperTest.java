package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExternalIdAuthenticationHelperTest {
    @Mock
    private UserService userService;

    private ExternalIdAuthenticationHelper helper;

    @Before
    public void setUp() {
        helper = new ExternalIdAuthenticationHelper(userService);
    }

    @Test
    public void getUserDetails_notFound() {
        when(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> helper.getUserDetails(Map.of("username", "bobuser"), false))
                .isInstanceOf(OAuth2AccessDeniedException.class).hasMessage("No user found matching username.");
    }

    @Test
    public void getUserDetails_found() {
        final var details = helper.getUserDetails(Map.of("username", "bobuser"), true);

        assertThat(details).isNotNull();
    }
}

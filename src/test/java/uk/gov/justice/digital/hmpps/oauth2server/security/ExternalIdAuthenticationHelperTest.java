package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ExternalIdAuthenticationHelperTest {
    @Mock
    private UserService userService;

    private ExternalIdAuthenticationHelper helper;

    @BeforeEach
    void setUp() {
        helper = new ExternalIdAuthenticationHelper(userService);
    }

    @Test
    void getUserDetails_notFound() {
        when(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> helper.getUserDetails(Map.of("username", "bobuser"), false))
                .isInstanceOf(OAuth2AccessDeniedException.class).hasMessage("No user found matching username.");
    }

    @Test
    void getUserDetails_found() {
        final var details = helper.getUserDetails(Map.of("username", "bobuser"), true);

        assertThat(details).isNotNull();
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DeliusUserDetailsServiceTest {
    @Mock
    private DeliusUserService userService;

    private DeliusUserDetailsService service;

    @Before
    public void setup() {
        service = new DeliusUserDetailsService(userService);
    }

    @Test
    public void testHappyUserPath() {

        final var user = buildStandardUser("ITAG_USER");
        when(userService.getDeliusUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isFalse();

        assertThat(((UserPersonDetails) itagUser).getName()).isEqualTo("Itag User");
    }

    @Test
    public void testUserNotFound() {

        when(userService.getDeliusUserByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("user")).isInstanceOf(UsernameNotFoundException.class);
    }

    private DeliusUserPersonDetails buildStandardUser(final String username) {
        return DeliusUserPersonDetails.builder()
                .username(username)
                .firstName("Itag")
                .enabled(false)
                .surname("User")
                .roles(List.of())
                .build();
    }
}

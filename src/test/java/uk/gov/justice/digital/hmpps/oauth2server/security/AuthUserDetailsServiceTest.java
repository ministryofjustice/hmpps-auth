package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
class AuthUserDetailsServiceTest {
    @Mock
    private AuthUserService userService;
    @Mock
    private EntityManager authEntityManager;

    private AuthUserDetailsService service;

    @BeforeEach
    void setup() {
        service = new AuthUserDetailsService(userService);
        ReflectionTestUtils.setField(service, "authEntityManager", authEntityManager);
    }

    @Test
    void testAuthEntityDetached() {

        final var user = buildAuthUser();
        when(userService.getAuthUserByUsername(user.getUsername())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        verify(authEntityManager).detach(user);

        assertThat(((UserPersonDetails) itagUser).getName()).isEqualTo("first last");
    }

    @Test
    void testAuthOnlyUser() {

        final var user = buildAuthUser();
        when(userService.getAuthUserByUsername(anyString())).thenReturn(Optional.of(user));

        final var itagUser = service.loadUserByUsername(user.getUsername());

        assertThat(itagUser).isNotNull();
        assertThat(itagUser.isAccountNonExpired()).isTrue();
        assertThat(itagUser.isAccountNonLocked()).isTrue();
        assertThat(itagUser.isCredentialsNonExpired()).isTrue();
        assertThat(itagUser.isEnabled()).isTrue();
    }

    @Test
    void testUserNotFound() {

        when(userService.getAuthUserByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("user")).isInstanceOf(UsernameNotFoundException.class);
    }

    private User buildAuthUser() {
        final var user = User.builder().username("user").email("email").verified(true).build();
        user.setPerson(new Person("first", "last"));
        user.setEnabled(true);
        user.setPasswordExpiry(LocalDateTime.now().plusDays(1));
        return user;
    }
}

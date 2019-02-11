package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserControllerTest {

    private final UserController userController = new UserController();

    @Test
    public void me() {
        final var principal = new TestingAuthenticationToken("principal", "credentials");
        assertThat(userController.me(principal)).isSameAs(principal);
    }

    @Test
    public void myRoles() {
        final var authorities = List.of(new SimpleGrantedAuthority("ROLE_BOB"), new SimpleGrantedAuthority("ROLE_JOE_FRED"));
        final var token = new UsernamePasswordAuthenticationToken("principal", "credentials", authorities);
        assertThat(userController.myRoles(token)).containsOnly(Map.of("roleCode", "BOB"), Map.of("roleCode", "JOE_FRED"));
    }

    @Test
    public void myRoles_noRoles() {
        final var token = new UsernamePasswordAuthenticationToken("principal", "credentials", Collections.emptyList());
        assertThat(userController.myRoles(token)).isEmpty();
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthAllRolesControllerTest {
    private static final Map<String, String> ALLOWED_AUTH_USER_ROLES = Map.of("ROLE_LICENCE_VARY", "Licence Variation", "ROLE_LICENCE_RO", "Licence Responsible Officer", "ROLE_GLOBAL_SEARCH", "Global Search");

    @Mock
    private AuthUserRoleService authUserRoleService;

    private AuthAllRolesController controller;

    @Before
    public void setUp() {
        controller = new AuthAllRolesController(authUserRoleService);
    }

    @Test
    public void allRoles() {
        when(authUserRoleService.getAllRoles()).thenReturn(List.of(new Authority("FRED", "FRED"), new Authority("GLOBAL_SEARCH", "Global Search")));

        final var response = controller.allRoles();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsOnly(
                new AuthUserRole("FRED", "FRED"),
                new AuthUserRole("Global Search", "GLOBAL_SEARCH"));
    }
}

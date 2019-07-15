package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthAllRolesControllerTest {
    @Mock
    private AuthUserRoleService authUserRoleService;

    private AuthAllRolesController controller;

    @Before
    public void setUp() {
        controller = new AuthAllRolesController(authUserRoleService);
    }

    @Test
    public void allRoles() {
        final var auth1 = new Authority("FRED", "FRED");
        final var auth2 = new Authority("GLOBAL_SEARCH", "Global Search");
        when(authUserRoleService.getAllRoles()).thenReturn(List.of(auth1, auth2));

        final var response = controller.allRoles();
        assertThat(response).containsOnly(new AuthUserRole(auth1), new AuthUserRole(auth2));
    }
}

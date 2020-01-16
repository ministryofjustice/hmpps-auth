package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthAllGroupsControllerTest {
    @Mock
    private AuthUserGroupService authUserGroupService;

    private AuthAllGroupsController controller;

    @Before
    public void setUp() {
        controller = new AuthAllGroupsController(authUserGroupService);
    }

    @Test
    public void allGroups() {
        final var group1 = new Group("FRED", "desc");
        final var group2 = new Group("GLOBAL_SEARCH", "desc2");
        final var groups = List.of(group2, group1);

        when(authUserGroupService.getAllGroups()).thenReturn(groups);

        final var response = controller.allGroups();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsOnly(
                new AuthUserGroup(group1),
                new AuthUserGroup(group2));
    }
}

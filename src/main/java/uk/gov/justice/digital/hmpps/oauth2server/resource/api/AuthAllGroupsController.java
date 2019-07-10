package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = {"/api/authgroups"})
public class AuthAllGroupsController {
    private final AuthUserGroupService authUserGroupService;

    public AuthAllGroupsController(final AuthUserGroupService authUserGroupService) {
        this.authUserGroupService = authUserGroupService;
    }

    @GetMapping("/api/authgroups")
    @ApiOperation(value = "Get all possible groups.", notes = "Get all groups allowed for auth users.", nickname = "allgroups",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthUserRole.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class)})
    public ResponseEntity<List<AuthUserGroup>> allGroups() {
        final var allGroups = authUserGroupService.getAllGroups();
        final var mappedGroups = allGroups.stream()
                .map(AuthUserGroup::new).collect(Collectors.toList());
        return ResponseEntity.ok(mappedGroups);
    }
}

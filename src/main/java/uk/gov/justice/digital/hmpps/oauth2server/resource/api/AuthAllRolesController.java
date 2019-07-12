package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = {"/api/authroles"})
public class AuthAllRolesController {
    private final AuthUserRoleService authUserRoleService;

    public AuthAllRolesController(final AuthUserRoleService authUserRoleService) {
        this.authUserRoleService = authUserRoleService;
    }

    @GetMapping("/api/authroles")
    @ApiOperation(value = "Get all possible roles.", notes = "Get all roles allowed for auth users.", nickname = "allroles",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthUserRole.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class)})
    public ResponseEntity<List<AuthUserRole>> allRoles() {
        final var allRoles = authUserRoleService.getAllRoles();
        final var mappedRoles = allRoles.stream().
                map(e -> new AuthUserRole(e.getRoleName(), e.getAuthorityName())).collect(Collectors.toList());
        return ResponseEntity.ok(mappedRoles);
    }
}

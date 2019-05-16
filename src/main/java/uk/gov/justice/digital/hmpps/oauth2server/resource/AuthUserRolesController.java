package uk.gov.justice.digital.hmpps.oauth2server.resource;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleException;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.AuthUserRoleExistsException;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

import java.security.Principal;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = {"/api/authuser/{username}/roles"})
public class AuthUserRolesController {
    private final UserService userService;
    private final AuthUserRoleService authUserRoleService;

    public AuthUserRolesController(final UserService userService, final AuthUserRoleService authUserRoleService) {
        this.userService = userService;
        this.authUserRoleService = authUserRoleService;
    }

    @GetMapping("/api/authuser/{username}/roles")
    @ApiOperation(value = "Get roles for user.", notes = "Get roles for user.", nickname = "roles",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthUserRole.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> roles(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username) {
        final var userOptional = userService.getAuthUserByUsername(username);
        final var allRoles = authUserRoleService.getAllRoles();

        return userOptional.
                map(u -> u.getAuthorities().stream().map(a -> {
                    final var authority = a.getAuthority();
                    final var roleName = allRoles.get(authority);
                    return new AuthUserRole(StringUtils.defaultString(roleName, a.getAuthorityName()), a.getAuthorityName());
                }).collect(Collectors.toSet())).
                map(Object.class::cast).
                map(ResponseEntity::ok).
                orElse(notFoundResponse(username));
    }

    @PutMapping("/api/authuser/{username}/roles/{role}")
    @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
    @ApiOperation(value = "Add role to user.", notes = "Add role to user.", nickname = "addRole",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Created"),
            @ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class),
            @ApiResponse(code = 409, message = "Role for user already exists.", response = ErrorDetail.class),
            @ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail.class)})
    public ResponseEntity<Object> addRole(
            @ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
            @ApiParam(value = "The role to be added to the user.", required = true) @PathVariable final String role,
            @ApiIgnore final Principal principal) {

        final var userOptional = userService.getAuthUserByUsername(username);
        return userOptional.map(UserEmail::getUsername).map(usernameInDb -> {
            try {
                authUserRoleService.addRole(usernameInDb, role, principal.getName());
                log.info("Add role succeeded for user {} and role {}", usernameInDb, role);
                return ResponseEntity.noContent().build();
            } catch (final AuthUserRoleExistsException e) {
                log.info("Add role failed for user {} for field {} with reason {}", usernameInDb, e.getField(), e.getErrorCode());
                return ResponseEntity.status(HttpStatus.CONFLICT).
                        <Object>body(new ErrorDetail("role.exists", String.format("Username %s already has role %s", usernameInDb, role), "role"));
            } catch (final AuthUserRoleException e) {
                log.info("Add role failed for user {} and role {} for field {} with reason {}", usernameInDb, role, e.getField(), e.getErrorCode());
                return ResponseEntity.badRequest().<Object>body(new ErrorDetail(String.format("%s.%s", e.getField(), e.getErrorCode()),
                        String.format("%s failed validation", e.getField()), e.getField()));
            }
        }).orElse(notFoundResponse(username));
    }

    @DeleteMapping("/api/authuser/{username}/roles/{role}")
    @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
    @ApiOperation(value = "Remove role from user.", notes = "Remove role from user.", nickname = "removeRole",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Removed"),
            @ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class),
            @ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail.class)})
    public ResponseEntity<Object> removeRole(
            @ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
            @ApiParam(value = "The role to be delete from the user.", required = true) @PathVariable final String role,
            @ApiIgnore final Principal principal) {

        final var userOptional = userService.getAuthUserByUsername(username);
        return userOptional.map(u -> {
            final var usernameInDb = u.getUsername();
            try {
                authUserRoleService.removeRole(usernameInDb, role, principal.getName());
            } catch (AuthUserRoleException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                        <Object>body(new ErrorDetail("role.missing", String.format("Username %s doesn't have the role %s", usernameInDb, role), "role"));
            }

            log.info("Remove role succeeded for user {} and role {}", usernameInDb, role);
            return ResponseEntity.noContent().build();
        }).orElse(notFoundResponse(username));
    }

    private ResponseEntity<Object> notFoundResponse(@PathVariable @ApiParam(value = "The username of the user.", required = true) final String username) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).
                body(new ErrorDetail("Not Found", String.format("Account for username %s not found", username), "username"));
    }
}

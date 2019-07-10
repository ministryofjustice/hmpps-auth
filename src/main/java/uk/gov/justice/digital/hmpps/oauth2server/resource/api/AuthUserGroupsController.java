package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupException;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupExistsException;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

import java.security.Principal;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = {"/api/authuser/{username}/groups"})
@AllArgsConstructor
public class AuthUserGroupsController {
    private final UserService userService;
    private final AuthUserGroupService authUserGroupService;

    @GetMapping("/api/authuser/{username}/groups")
    @ApiOperation(value = "Get groups for user.", notes = "Get groups for user.", nickname = "groups",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthUserGroup.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> groups(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username) {
        final var userOptional = authUserGroupService.getAuthUserGroups(username);
        return userOptional.
                map(g -> g.stream().map(AuthUserGroup::new).collect(Collectors.toSet())).
                map(Object.class::cast).
                map(ResponseEntity::ok).
                orElse(notFoundResponse(username));
    }

    @PutMapping("/api/authuser/{username}/groups/{group}")
    @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
    @ApiOperation(value = "Add group to user.", notes = "Add group to user.", nickname = "addGroup",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Created"),
            @ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class),
            @ApiResponse(code = 409, message = "Group for user already exists.", response = ErrorDetail.class),
            @ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail.class)})
    public ResponseEntity<Object> addGroup(
            @ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
            @ApiParam(value = "The group to be added to the user.", required = true) @PathVariable final String group,
            @ApiIgnore final Principal principal) {

        final var userOptional = userService.getAuthUserByUsername(username);
        return userOptional.map(UserEmail::getUsername).map(usernameInDb -> {
            try {
                authUserGroupService.addGroup(usernameInDb, group, principal.getName());
                log.info("Add group succeeded for user {} and group {}", usernameInDb, group);
                return ResponseEntity.noContent().build();
            } catch (final AuthUserGroupExistsException e) {
                log.info("Add group failed for user {} for field {} with reason {}", usernameInDb, e.getField(), e.getErrorCode());
                return ResponseEntity.status(HttpStatus.CONFLICT).
                        <Object>body(new ErrorDetail("group.exists", String.format("Username %s already has group %s", usernameInDb, group), "group"));
            } catch (final AuthUserGroupException e) {
                log.info("Add group failed for user {} and group {} for field {} with reason {}", usernameInDb, group, e.getField(), e.getErrorCode());
                return ResponseEntity.badRequest().<Object>body(new ErrorDetail(String.format("%s.%s", e.getField(), e.getErrorCode()),
                        String.format("%s failed validation", e.getField()), e.getField()));
            }
        }).orElse(notFoundResponse(username));
    }

    @DeleteMapping("/api/authuser/{username}/groups/{group}")
    @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
    @ApiOperation(value = "Remove group from user.", notes = "Remove group from user.", nickname = "removeGroup",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Removed"),
            @ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class),
            @ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail.class)})
    public ResponseEntity<Object> removeGroup(
            @ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
            @ApiParam(value = "The group to be delete from the user.", required = true) @PathVariable final String group,
            @ApiIgnore final Principal principal) {

        final var userOptional = userService.getAuthUserByUsername(username);
        return userOptional.map(u -> {
            final var usernameInDb = u.getUsername();
            try {
                authUserGroupService.removeGroup(usernameInDb, group, principal.getName());
            } catch (AuthUserGroupException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                        <Object>body(new ErrorDetail("group.missing", String.format("Username %s doesn't have the group %s", usernameInDb, group), "group"));
            }

            log.info("Remove group succeeded for user {} and group {}", usernameInDb, group);
            return ResponseEntity.noContent().build();
        }).orElse(notFoundResponse(username));
    }

    private ResponseEntity<Object> notFoundResponse(@PathVariable @ApiParam(value = "The username of the user.", required = true) final String username) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).
                body(new ErrorDetail("Not Found", String.format("Account for username %s not found", username), "username"));
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.resource;

import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserRole;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

import java.security.Principal;
import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@Api(tags = {"/api/user"})
public class UserController {
    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/user/me")
    @ApiOperation(value = "Current user detail.", notes = "Current user detail.", nickname = "getMyUserInformation",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized", response = ErrorDetail.class)})
    public UserDetail me(@ApiIgnore final Principal principal) {
        final var user = userService.findUser(principal.getName());

        return user.map(this::mapUser).orElse(UserDetail.fromUsername(principal.getName()));
    }

    @GetMapping("/api/user/me/roles")
    @ApiOperation(value = "List of roles for current user.", notes = "List of roles for current user.", nickname = "getMyRoles",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserRole.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized", response = ErrorDetail.class)})
    public Collection<UserRole> myRoles(@ApiIgnore final Authentication authentication) {
        return authentication.getAuthorities().stream().
                map(a -> new UserRole(a.getAuthority().substring(5))). // remove ROLE_
                collect(Collectors.toList());
    }

    @GetMapping("/api/user/{username}")
    @ApiOperation(value = "User detail.", notes = "User detail.", nickname = "getUserDetails",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> user(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username) {
        final var user = userService.findUser(username);

        return user.map(this::mapUser).map(Object.class::cast).map(ResponseEntity::ok).
                orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundBody(username)));
    }

    private Object notFoundBody(final String username) {
        return new ErrorDetail("Not Found", String.format("Account for username %s not found", username));
    }

    private UserDetail mapUser(final UserPersonDetails u) {
        return UserDetail.fromPerson(u);
    }
}

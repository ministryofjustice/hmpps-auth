package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;
import uk.gov.justice.digital.hmpps.oauth2server.model.EmailAddress;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserRole;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

import java.security.Principal;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = {"/api/user"})
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/api/user/me")
    @ApiOperation(value = "Current user detail.", notes = "Current user detail.", nickname = "getMyUserInformation",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized", response = ErrorDetail.class)})
    public UserDetail me(@ApiIgnore final Principal principal) {
        final var user = userService.findUser(principal.getName());

        return user.map(UserDetail::fromPerson).orElse(UserDetail.fromUsername(principal.getName()));
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

        return user.map(UserDetail::fromPerson).map(Object.class::cast).map(ResponseEntity::ok).
                orElse(notFoundResponse(username));
    }

    @GetMapping("/api/user/{username}/email")
    @ApiOperation(value = "Email address for user", notes = "Verified email address for user", nickname = "getUserEmail",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserDetail.class),
            @ApiResponse(code = 204, message = "No content.  No verified email address found for user"),
            @ApiResponse(code = 404, message = "User not found.  The user doesn't exist in auth so could have never logged in", response = ErrorDetail.class)})
    public ResponseEntity<Object> getUserEmail(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username) {
        final var userEmail = userService.findUserEmail(username);

        if (userEmail.isEmpty()) {
            return notFoundResponse(username);
        }

        final var email = userEmail.get();

        return email.isVerified() ? ResponseEntity.ok(EmailAddress.fromUserEmail(email)) : ResponseEntity.noContent().build();
    }


    private ResponseEntity<Object> notFoundResponse(final String username) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).
                body(new ErrorDetail("Not Found", String.format("Account for username %s not found", username), "username"));
    }
}

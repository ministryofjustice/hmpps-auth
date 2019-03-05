package uk.gov.justice.digital.hmpps.oauth2server.resource;

import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.CreateUserService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.CreateUserService.CreateUserException;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserRole;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = {"/api/user"})
public class UserController {
    private final UserService userService;
    private final CreateUserService createUserService;
    private final boolean smokeTestEnabled;

    public UserController(final UserService userService, final CreateUserService createUserService,
                          @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.userService = userService;
        this.createUserService = createUserService;
        this.smokeTestEnabled = smokeTestEnabled;
    }

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
                orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundBody(username)));
    }

    @PutMapping("/api/user/{username}")
    @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
    @ApiOperation(value = "Create user.", notes = "Create user.", nickname = "createUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 409, message = "User already exists.", response = ErrorDetail.class),
            @ApiResponse(code = 500, message = "Server exception e.g. failed to call notify.", response = ErrorDetail.class)})
    public ResponseEntity<Object> createUser(
            @ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
            @ApiParam(value = "Details of the user to be created.", required = true) @RequestBody final CreateUser createUser,
            @ApiIgnore final HttpServletRequest request) throws NotificationClientException {

        final var user = StringUtils.isNotBlank(username) ? userService.findUser(username) : Optional.empty();

        // check that we're not asked to create a user that is already in nomis or auth
        if (user.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDetail("username.exists", String.format("Username %s already exists", username)));
        }

        // new user
        try {
            final var requestURL = request.getRequestURL();
            final var setPasswordUrl = requestURL.toString().replaceFirst("/api/user/.*", "/initial-password?token=");
            final var resetLink = createUserService.createUser(username, createUser.getEmail(), createUser.getFirstName(), createUser.getLastName(), setPasswordUrl);

            log.info("Create user succeeded for user {}", username);
            if (smokeTestEnabled) {
                return ResponseEntity.ok(resetLink);
            }
            return ResponseEntity.noContent().build();
        } catch (final CreateUserException e) {
            log.info("Create user failed for user {} for field {} with reason {}", username, e.getField(), e.getErrorCode());
            return ResponseEntity.badRequest().body(new ErrorDetail(String.format("%s.%s", e.getField(), e.getErrorCode()),
                    String.format("%s failed validation", e.getField())));
        } catch (final VerifyEmailException e) {
            log.info("Create user failed for user {} for field email with reason {}", username, e.getReason());
            return ResponseEntity.badRequest().body(new ErrorDetail(String.format("email.%s", e.getReason()), "Email address failed validation"));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUser {
        @ApiModelProperty(required = true, value = "Email address", example = "john.smith@digital.justice.gov.uk", position = 1)
        private String email;
        @ApiModelProperty(required = true, value = "First name", example = "John", position = 2)
        private String firstName;
        @ApiModelProperty(required = true, value = "Last name", example = "Smith", position = 3)
        private String lastName;
    }

    private Object notFoundBody(final String username) {
        return new ErrorDetail("Not Found", String.format("Account for username %s not found", username));
    }
}

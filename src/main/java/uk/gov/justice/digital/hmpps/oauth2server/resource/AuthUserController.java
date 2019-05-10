package uk.gov.justice.digital.hmpps.oauth2server.resource;

import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.CreateUserService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.CreateUserService.CreateUserException;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientException;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Api(tags = {"/api/authuser"})
public class AuthUserController {
    private final UserService userService;
    private final CreateUserService createUserService;
    private final boolean smokeTestEnabled;

    public AuthUserController(final UserService userService, final CreateUserService createUserService,
                              @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.userService = userService;
        this.createUserService = createUserService;
        this.smokeTestEnabled = smokeTestEnabled;
    }

    @GetMapping("/api/authuser/{username}")
    @ApiOperation(value = "User detail.", notes = "User detail.", nickname = "getUserDetails",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> user(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username) {
        final var user = userService.getAuthUserByUsername(username);

        return user.map(AuthUser::fromUserEmail).map(Object.class::cast).map(ResponseEntity::ok).
                orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundBody(username)));
    }

    @GetMapping("/api/authuser")
    @ApiOperation(value = "Search for a user.", notes = "Search for a user.", nickname = "searchForUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthUser.class, responseContainer = "List"),
            @ApiResponse(code = 204, message = "No users found."),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class)})
    public ResponseEntity<Object> searchForUser(@ApiParam(value = "The email address of the user.", required = true) @RequestParam final String email) {
        final var users = userService.findAuthUsersByEmail(email).stream().map(AuthUser::fromUserEmail).collect(Collectors.toList());

        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @PutMapping("/api/authuser/{username}")
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
            @ApiIgnore final HttpServletRequest request,
            @ApiIgnore final Principal principal) throws NotificationClientException {

        final var user = StringUtils.isNotBlank(username) ? userService.findUser(StringUtils.trim(username)) : Optional.empty();

        // check that we're not asked to create a user that is already in nomis or auth
        if (user.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDetail("username.exists", String.format("Username %s already exists", username), "username"));
        }

        // new user
        try {
            final var requestURL = request.getRequestURL();
            final var setPasswordUrl = requestURL.toString().replaceFirst("/api/authuser/.*", "/initial-password?token=");
            final var resetLink = createUserService.createUser(StringUtils.trim(username), createUser.getEmail(), createUser.getFirstName(), createUser.getLastName(), createUser.getAdditionalRoles(), setPasswordUrl, principal.getName());

            log.info("Create user succeeded for user {}", username);
            if (smokeTestEnabled) {
                return ResponseEntity.ok(resetLink);
            }
            return ResponseEntity.noContent().build();
        } catch (final CreateUserException e) {
            log.info("Create user failed for user {} for field {} with reason {}", username, e.getField(), e.getErrorCode());
            return ResponseEntity.badRequest().body(new ErrorDetail(String.format("%s.%s", e.getField(), e.getErrorCode()),
                    String.format("%s failed validation", e.getField()), e.getField()));
        } catch (final VerifyEmailException e) {
            log.info("Create user failed for user {} for field email with reason {}", username, e.getReason());
            return ResponseEntity.badRequest().body(new ErrorDetail(String.format("email.%s", e.getReason()), "Email address failed validation", "email"));
        }
    }

    @PutMapping("/api/authuser/{username}/enable")
    @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
    @ApiOperation(value = "Enable a user.", notes = "Enable a user.", nickname = "enableUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> enableUser(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
                                             @ApiIgnore final Principal principal) {
        try {
            userService.enableUser(username, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (final EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/api/authuser/{username}/disable")
    @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
    @ApiOperation(value = "Disable a user.", notes = "Disable a user.", nickname = "disableUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = UserDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> disableUser(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
                                              @ApiIgnore final Principal principal) {
        try {
            userService.disableUser(username, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (final EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CreateUser {
        @ApiModelProperty(required = true, value = "Email address", example = "nomis.user@someagency.justice.gov.uk", position = 1)
        private String email;
        @ApiModelProperty(required = true, value = "First name", example = "Nomis", position = 2)
        private String firstName;
        @ApiModelProperty(required = true, value = "Last name", example = "User", position = 3)
        private String lastName;
        @ApiModelProperty(value = "Additional roles", example = "[ROLE_LICENCE_VARY]", dataType = "Set", position = 4)
        private Set<String> additionalRoles;

        Set<String> getAdditionalRoles() {
            return additionalRoles == null ? Collections.emptySet() : additionalRoles;
        }
    }

    @Data
    @Builder
    static class AuthUser {
        @ApiModelProperty(required = true, value = "Username", example = "authuser", position = 1)
        private String username;
        @ApiModelProperty(required = true, value = "Email address", example = "auth.user@someagency.justice.gov.uk", position = 2)
        private String email;
        @ApiModelProperty(required = true, value = "First name", example = "Auth", position = 3)
        private String firstName;
        @ApiModelProperty(required = true, value = "Last name", example = "User", position = 4)
        private String lastName;
        @ApiModelProperty(required = true, value = "Account is locked due to incorrect password attempts", example = "true", position = 5)
        private boolean locked;
        @ApiModelProperty(required = true, value = "Account is enabled", example = "false", position = 6)
        private boolean enabled;

        private static AuthUser fromUserEmail(final UserEmail user) {
            return AuthUser.builder().username(user.getUsername()).email(user.getEmail()).firstName(user.getFirstName()).lastName(user.getPerson().getLastName()).locked(user.isLocked()).enabled(user.isEnabled()).build();
        }
    }

    private Object notFoundBody(final String username) {
        return new ErrorDetail("Not Found", String.format("Account for username %s not found", username), "username");
    }
}

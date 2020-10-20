package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException;
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup;
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientException;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Sort.Direction.ASC;

@Slf4j
@RestController
@Api(tags = {"/api/authuser"})
public class AuthUserController {
    private final UserService userService;
    private final AuthUserService authUserService;
    private final AuthUserGroupService authUserGroupService;
    private final boolean smokeTestEnabled;

    public AuthUserController(final UserService userService, final AuthUserService authUserService,
                              final AuthUserGroupService authUserGroupService, @Value("${application.smoketest.enabled}") final boolean smokeTestEnabled) {
        this.userService = userService;
        this.authUserService = authUserService;
        this.authUserGroupService = authUserGroupService;
        this.smokeTestEnabled = smokeTestEnabled;
    }

    @GetMapping("/api/authuser/{username}")
    @ApiOperation(value = "User detail.", notes = "User detail.", nickname = "getUserDetails",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthUser.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> user(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username) {
        final var user = authUserService.getAuthUserByUsername(username);

        return user.map(AuthUser::fromUser).map(Object.class::cast).map(ResponseEntity::ok).
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
        final var users = authUserService.findAuthUsersByEmail(email).stream().map(AuthUser::fromUser).collect(Collectors.toList());

        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/api/authuser/search")
    @ApiOperation(value = "Search for a user.", notes = "Search for a user.", nickname = "searchForUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthUser.class, responseContainer = "List"),
            @ApiResponse(code = 204, message = "No users found."),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class)})
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", dataType = "int", paramType = "query",
                    value = "Results page you want to retrieve (0..N)", example = "0", defaultValue = "0"),
            @ApiImplicitParam(name = "size", dataType = "int", paramType = "query",
                    value = "Number of records per page.", example = "10", defaultValue = "10"),
            @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                    value = "Sort column and direction, eg sort=lastName,desc")})
    public ResponseEntity<Object> searchForUser(
            @ApiParam(value = "The username, email or name of the user.", example = "j smith") @RequestParam(required = false) final String name,
            @ApiParam(value = "The role of the user.") @RequestParam(required = false) final String role,
            @ApiParam(value = "The group of the user.") @RequestParam(required = false) final String group,
            @PageableDefault(sort = {"username"}, direction = ASC) final Pageable pageable) {
        final var users = authUserService.findAuthUsers(name, role, group, pageable).stream().map(AuthUser::fromUser).collect(Collectors.toList());

        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @GetMapping("/api/authuser/me/assignable-groups")
    @ApiOperation(value = "Get list of assignable groups.", notes = "Get list of groups that can be assigned by the current user.", nickname = "assignableGroups",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthUserGroup.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class)})
    public List<AuthUserGroup> assignableGroups(@ApiIgnore final Authentication authentication) {
        final var groups = authUserGroupService.getAssignableGroups(authentication.getName(), authentication.getAuthorities());
        return groups.stream().map(AuthUserGroup::new).collect(Collectors.toList());
    }

    @PutMapping("/api/authuser/{username}")
    @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
    @ApiOperation(value = "Create user.", notes = "Create user.", nickname = "createUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 409, message = "User or email already exists.", response = ErrorDetail.class),
            @ApiResponse(code = 500, message = "Server exception e.g. failed to call notify.", response = ErrorDetail.class)})
    public ResponseEntity<Object> createUser(
            @ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
            @ApiParam(value = "Details of the user to be created.", required = true) @RequestBody final CreateUser createUser,
            @ApiParam(value = "Details of the user to be created.") @RequestParam(required = false) final boolean enforceUniqueEmail,
            @ApiIgnore final HttpServletRequest request,
            @ApiIgnore final Authentication authentication) throws NotificationClientException {

        final var user = StringUtils.isNotBlank(username) ? userService.findMasterUserPersonDetails(StringUtils.trim(username)) : Optional.empty();

        // check that we're not asked to create a user that is already in nomis, auth or delius
        if (user.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDetail("username.exists", String.format("User %s already exists", username), "username"));
        }
        if (enforceUniqueEmail && !authUserService.findAuthUsersByEmail(createUser.getEmail()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDetail("email.exists", String.format("User %s already exists", createUser.getEmail()), "email"));
        }

        var mergedGroups = new HashSet<String>();
        if (createUser.getGroupCodes() != null) {
            mergedGroups.addAll(createUser.getGroupCodes());
        }

        if (createUser.getGroupCode() != null) {
            mergedGroups.add(createUser.getGroupCode());
        }

        // new user
        try {
            final var setPasswordUrl = createInitialPasswordUrl(request);
            final var resetLink = authUserService.createUser(StringUtils.trim(username), createUser.getEmail(),
                    createUser.getFirstName(), createUser.getLastName(), mergedGroups,
                    setPasswordUrl, authentication.getName(), authentication.getAuthorities());

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

    private String createInitialPasswordUrl(@ApiIgnore final HttpServletRequest request) {
        final var requestURL = request.getRequestURL();
        return requestURL.toString().replaceFirst("/api/authuser/.*", "/initial-password?token=");
    }

    @PutMapping("/api/authuser/{username}/enable")
    @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
    @ApiOperation(value = "Enable a user.", notes = "Enable a user.", nickname = "enableUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 403, message = "Unable to enable user, the user is not within one of your groups", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> enableUser(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
                                             @ApiIgnore final Authentication authentication) {

        final var userOptional = authUserService.getAuthUserByUsername(username);
        return userOptional.map(u -> {
            final var usernameInDb = u.getUsername();
            try {
                authUserService.enableUser(usernameInDb, authentication.getName(), authentication.getAuthorities());
                return ResponseEntity.noContent().build();
            } catch (final AuthUserGroupRelationshipException e) {
                log.info("enable user failed  with reason {}", e.getErrorCode());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).
                        <Object>body(new ErrorDetail("unable to maintain user", "Unable to enable user, the user is not within one of your groups", "groups"));
            } catch (final EntityNotFoundException e) {
                return ResponseEntity.notFound().build();
            }
        }).orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", username)));
    }

    @PutMapping("/api/authuser/{username}/disable")
    @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
    @ApiOperation(value = "Disable a user.", notes = "Disable a user.", nickname = "disableUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 403, message = "Unable to disable user, the user is not within one of your groups", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> disableUser(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
                                              @ApiIgnore final Authentication authentication) {
        final var userOptional = authUserService.getAuthUserByUsername(username);
        return userOptional.map(u -> {
            final var usernameInDb = u.getUsername();

            try {
                authUserService.disableUser(usernameInDb, authentication.getName(), authentication.getAuthorities());
                return ResponseEntity.noContent().build();
            } catch (final AuthUserGroupRelationshipException e) {
                log.info("Disable user failed  with reason {}", e.getErrorCode());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).
                        <Object>body(new ErrorDetail("unable to maintain user", "Unable to disable user, the user is not within one of your groups", "groups"));
            } catch (final EntityNotFoundException e) {
                return ResponseEntity.notFound().build();
            }
        }).orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", username)));
    }

    @PostMapping("/api/authuser/{username}")
    @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
    @ApiOperation(value = "Amend a user.", notes = "Amend a user.", nickname = "amendUser",
            consumes = "application/json", produces = "application/json")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 400, message = "Bad request e.g. if validation failed or if the amendments are disallowed", response = ErrorDetail.class),
            @ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail.class),
            @ApiResponse(code = 403, message = "Unable to amend user, the user is not within one of your groups", response = ErrorDetail.class),
            @ApiResponse(code = 404, message = "User not found.", response = ErrorDetail.class)})
    public ResponseEntity<Object> amendUser(@ApiParam(value = "The username of the user.", required = true) @PathVariable final String username,
                                            @RequestBody final AmendUser amendUser,
                                            @ApiIgnore final HttpServletRequest request,
                                            @ApiIgnore final Authentication authentication) throws NotificationClientException {
        try {
            final var setPasswordUrl = createInitialPasswordUrl(request);
            final var resetLink = authUserService.amendUserEmail(username, amendUser.getEmail(), setPasswordUrl, authentication.getName(), authentication.getAuthorities(), EmailType.PRIMARY);
            log.info("Amend user succeeded for user {}", username);
            if (smokeTestEnabled) {
                return ResponseEntity.ok(resetLink);
            }
            return ResponseEntity.noContent().build();
        } catch (final EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (final VerifyEmailException e) {
            log.info("Amend user failed for user {} for field email with reason {}", username, e.getReason());
            return ResponseEntity.badRequest().body(new ErrorDetail(String.format("email.%s", e.getReason()), "Email address failed validation", "email"));
        } catch (final AuthUserGroupRelationshipException e) {
            log.info("enable user failed  with reason {}", e.getErrorCode());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).
                    body(new ErrorDetail("unable to maintain user", "Unable to amend user, the user is not within one of your groups", "groups"));
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
        @ApiModelProperty(value = "Initial group, required for group managers", example = "SITE_1_GROUP_1", position = 4)
        private String groupCode;
        @ApiModelProperty(value = "Initial groups, can be used if multiple initial groups required", example = "[\"SITE_1_GROUP_1\", \"SITE_1_GROUP_2\"]", position = 5)
        private Set<String> groupCodes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class AmendUser {
        @ApiModelProperty(required = true, value = "Email address", example = "nomis.user@someagency.justice.gov.uk", position = 1)
        private String email;
    }

    @Data
    @Builder
    static class AuthUser {
        @ApiModelProperty(required = true, value = "User ID", example = "91229A16-B5F4-4784-942E-A484A97AC865", position = 1)
        private String userId;
        @ApiModelProperty(required = true, value = "Username", example = "authuser", position = 2)
        private String username;
        @ApiModelProperty(required = true, value = "Email address", example = "auth.user@someagency.justice.gov.uk", position = 3)
        private String email;
        @ApiModelProperty(required = true, value = "First name", example = "Auth", position = 4)
        private String firstName;
        @ApiModelProperty(required = true, value = "Last name", example = "User", position = 5)
        private String lastName;
        @ApiModelProperty(required = true, value = "Account is locked due to incorrect password attempts", example = "true", position = 6)
        private boolean locked;
        @ApiModelProperty(required = true, value = "Account is enabled", example = "false", position = 7)
        private boolean enabled;
        @ApiModelProperty(required = true, value = "Email address has been verified", example = "false", position = 8)
        private boolean verified;
        @ApiModelProperty(required = true, value = "Last time user logged in", example = "01/01/2001", position = 9)
        private LocalDateTime lastLoggedIn;


        private static AuthUser fromUser(final User user) {
            return AuthUser.builder()
                    .userId(user.getId().toString())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getPerson().getLastName())
                    .locked(user.isLocked())
                    .enabled(user.isEnabled())
                    .verified(user.isVerified())
                    .lastLoggedIn(user.getLastLoggedIn())
                    .build();
        }
    }

    private Object notFoundBody(final String username) {
        return new ErrorDetail("Not Found", String.format("Account for username %s not found", username), "username");
    }

}

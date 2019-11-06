package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.*;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException;
import uk.gov.justice.digital.hmpps.oauth2server.utils.EmailHelper;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
public class AuthUserService {
    private final UserRepository userRepository;
    private final NotificationClientApi notificationClient;
    private final TelemetryClient telemetryClient;
    private final VerifyEmailService verifyEmailService;
    private final AuthUserGroupService authUserGroupService;
    private final MaintainUserCheck maintainUserCheck;
    private final String initialPasswordTemplateId;

    // Data item field size validation checks
    private static final int MAX_LENGTH_USERNAME = 30;
    private static final int MAX_LENGTH_FIRST_NAME = 50;
    private static final int MAX_LENGTH_LAST_NAME = 50;
    private static final int MIN_LENGTH_USERNAME = 6;
    private static final int MIN_LENGTH_FIRST_NAME = 2;
    private static final int MIN_LENGTH_LAST_NAME = 2;

    public AuthUserService(final UserRepository userRepository,
                           final NotificationClientApi notificationClient,
                           final TelemetryClient telemetryClient,
                           final VerifyEmailService verifyEmailService,
                           final AuthUserGroupService authUserGroupService,
                           final MaintainUserCheck maintainUserCheck,
                           @Value("${application.notify.create-initial-password.template}") final String initialPasswordTemplateId) {
        this.userRepository = userRepository;
        this.notificationClient = notificationClient;
        this.telemetryClient = telemetryClient;
        this.verifyEmailService = verifyEmailService;
        this.authUserGroupService = authUserGroupService;
        this.maintainUserCheck = maintainUserCheck;
        this.initialPasswordTemplateId = initialPasswordTemplateId;
    }

    @Transactional(transactionManager = "authTransactionManager")
    public String createUser(final String usernameInput, final String emailInput, final String firstName, final String lastName,
                             final String groupCode, final String url, final String creator, final Collection<? extends GrantedAuthority> authorities)
            throws CreateUserException, NotificationClientException, VerifyEmailException {
        // ensure username always uppercase
        final var username = StringUtils.upperCase(usernameInput);
        // and use email helper to format input email
        final var email = EmailHelper.format(emailInput);

        // validate
        validate(username, email, firstName, lastName);

        // get the initial group to assign to - only allowed to be empty if super user
        final var group = getInitialGroup(groupCode, creator, authorities);

        // create the user
        final var person = new Person(firstName, lastName);

        // obtain list of authorities that should be assigned for group
        final var roles = group.map(Group::getAssignableRoles)
                .map(gar -> gar.stream()
                        .filter(GroupAssignableRole::isAutomatic)
                        .map(GroupAssignableRole::getRole)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        final var groups = group.map(Set::of).orElse(Set.of());

        final var user = User.builder()
                .username(username)
                .email(email)
                .enabled(true)
                .master(true)
                .person(person)
                .authorities(roles)
                .groups(groups).build();
        return saveAndSendInitialEmail(url, user, creator, "AuthUserCreate");
    }

    public Page<User> findAuthUsers(final String name, final String roleCode, final String groupCode, final Pageable pageable) {
        final var userFilter = UserFilter.builder().name(name).roleCode(roleCode).groupCode(groupCode).build();
        return userRepository.findAll(userFilter, pageable);
    }

    private Optional<Group> getInitialGroup(final String groupCode, final String creator, final Collection<? extends GrantedAuthority> authorities) throws CreateUserException {
        if (StringUtils.isEmpty(groupCode)) {
            if (authorities.stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_MAINTAIN_OAUTH_USERS"::equals)) {
                return Optional.empty();
            } else throw new CreateUserException("groupCode", "missing");
        }
        final var authUserGroups = authUserGroupService.getAssignableGroups(creator, authorities);
        return Optional.of(authUserGroups.stream().filter(g -> g.getGroupCode().equals(groupCode)).findFirst().orElseThrow(() -> new CreateUserException("groupCode", "notfound")));
    }

    private String saveAndSendInitialEmail(final String url, final User user, final String creator, final String eventPrefix) throws NotificationClientException {
        // then the reset token
        final var userToken = user.createToken(TokenType.RESET);
        // give users more time to do the reset
        userToken.setTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        final var setPasswordLink = url + userToken.getToken();
        final var username = user.getUsername();
        final var email = user.getEmail();
        final var parameters = Map.of("firstName", user.getFirstName(), "resetLink", setPasswordLink);

        // send the email
        try {
            log.info("Sending initial set password to notify for user {}", username);
            notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null);
            telemetryClient.trackEvent(String.format("%sSuccess", eventPrefix), Map.of("username", username, "admin", creator), null);
        } catch (final NotificationClientException e) {
            final var reason = (e.getCause() != null ? e.getCause() : e).getClass().getSimpleName();
            log.warn("Failed to send create user notify for user {}", username, e);
            telemetryClient.trackEvent(String.format("%sFailure", eventPrefix), Map.of("username", username, "reason", reason, "admin", creator), null);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null, null);
                telemetryClient.trackEvent(String.format("%sSuccess", eventPrefix), Map.of("username", username, "admin", creator), null);
            }
            throw e;
        }

        // return the reset link to the controller
        return setPasswordLink;
    }

    @Transactional(transactionManager = "authTransactionManager")
    public String amendUser(final String usernameInput, final String emailAddressInput, final String url, final String admin, final Collection<? extends GrantedAuthority> authorities)
            throws AmendUserException, VerifyEmailException, NotificationClientException, AuthUserGroupRelationshipException {
        final var username = StringUtils.upperCase(usernameInput);
        final var email = EmailHelper.format(emailAddressInput);

        final var user = userRepository.findByUsernameAndMasterIsTrue(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", username)));

        maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, user);

        // if unverified and password not set then still in the initial state
        if (user.isVerified() || user.getPassword() != null) {
            throw new AmendUserException("email", "notinitial");
        }
        verifyEmailService.validateEmailAddress(email);
        user.setEmail(email);
        return saveAndSendInitialEmail(url, user, admin, "AuthUserAmend");
    }

    private void validate(final String username, final String email, final String firstName, final String lastName)
            throws CreateUserException, VerifyEmailException {

        if (StringUtils.length(username) < MIN_LENGTH_USERNAME) {
            throw new CreateUserException("username", "length");
        }
        if (StringUtils.length(username) > MAX_LENGTH_USERNAME) {
            throw new CreateUserException("username", "maxlength");
        }
        if (!username.matches("^[A-Z0-9_]*$")) {
            throw new CreateUserException("username", "format");
        }
        if (StringUtils.length(firstName) < MIN_LENGTH_FIRST_NAME) {
            throw new CreateUserException("firstName", "length");
        }
        if (StringUtils.length(firstName) > MAX_LENGTH_FIRST_NAME) {
            throw new CreateUserException("firstName", "maxlength");
        }
        if (StringUtils.length(lastName) < MIN_LENGTH_LAST_NAME) {
            throw new CreateUserException("lastName", "length");
        }
        if (StringUtils.length(lastName) > MAX_LENGTH_LAST_NAME) {
            throw new CreateUserException("lastName", "maxlength");
        }

        verifyEmailService.validateEmailAddress(email);
    }

    @Getter
    public static class CreateUserException extends Exception {
        private final String errorCode;
        private final String field;

        public CreateUserException(final String field, final String errorCode) {
            super(String.format("Create user failed for field %s with reason: %s", field, errorCode));

            this.field = field;
            this.errorCode = errorCode;
        }
    }

    @Getter
    public static class AmendUserException extends Exception {
        private final String errorCode;
        private final String field;

        public AmendUserException(final String field, final String errorCode) {
            super(String.format("Amend user failed for field %s with reason: %s", field, errorCode));

            this.field = field;
            this.errorCode = errorCode;
        }
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.google.common.collect.Sets;
import com.microsoft.applicationinsights.TelemetryClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService.ALLOWED_AUTH_USER_ROLES;

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
public class AuthUserService {
    private final UserTokenRepository userTokenRepository;
    private final UserEmailRepository userEmailRepository;
    private final NotificationClientApi notificationClient;
    private final TelemetryClient telemetryClient;
    private final VerifyEmailService verifyEmailService;
    private final String initialPasswordTemplateId;

    public AuthUserService(final UserTokenRepository userTokenRepository,
                           final UserEmailRepository userEmailRepository,
                           final NotificationClientApi notificationClient,
                           final TelemetryClient telemetryClient,
                           final VerifyEmailService verifyEmailService, @Value("${application.notify.create-initial-password.template}") final String initialPasswordTemplateId) {
        this.userTokenRepository = userTokenRepository;
        this.userEmailRepository = userEmailRepository;
        this.notificationClient = notificationClient;
        this.telemetryClient = telemetryClient;
        this.verifyEmailService = verifyEmailService;
        this.initialPasswordTemplateId = initialPasswordTemplateId;
    }

    @Transactional(transactionManager = "authTransactionManager")
    public String createUser(final String usernameInput, final String emailInput, final String firstName, final String lastName,
                             final Set<String> additionalRoles, final String url, final String creator)
            throws CreateUserException, NotificationClientException, VerifyEmailException {
        // ensure username always uppercase
        final var username = StringUtils.upperCase(usernameInput);
        // and that email is always lowercase
        final var email = StringUtils.lowerCase(emailInput);

        // validate
        validate(username, email, firstName, lastName);

        // create the user
        final var person = new Person(username, firstName, lastName);

        // create list of authorities
        final var authorities = calculateRoles(additionalRoles);
        final var user = new UserEmail(username, null, email, false, false, true, true, LocalDateTime.now(), person, authorities, Set.of());
        return saveAndSendInitialEmail(url, user, creator, "AuthUserCreate");
    }

    private String saveAndSendInitialEmail(final String url, final UserEmail user, final String creator, final String eventPrefix) throws NotificationClientException {
        userEmailRepository.save(user);

        // then the reset token
        final var userToken = new UserToken(TokenType.RESET, user);
        // give users more time to do the reset
        userToken.setTokenExpiry(LocalDateTime.now().plusDays(7));
        userTokenRepository.save(userToken);

        final var setPasswordLink = url + userToken.getToken();
        final var username = user.getUsername();
        final var email = user.getEmail();
        final var parameters = Map.of("firstName", user.getFirstName(), "resetLink", setPasswordLink, "username", username);

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
    public String amendUser(final String usernameInput, final String emailAddressInput, final String url, final String admin) throws AmendUserException, VerifyEmailException, NotificationClientException {
        final var username = StringUtils.upperCase(usernameInput);
        final var email = StringUtils.lowerCase(emailAddressInput);

        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", username)));
        // if unverified and password not set then still in the initial state
        if (userEmail.isVerified() || userEmail.getPassword() != null) {
            throw new AmendUserException("email", "notinitial");
        }
        verifyEmailService.validateEmailAddress(email);
        userEmail.setEmail(email);
        return saveAndSendInitialEmail(url, userEmail, admin, "AuthUserAmend");
    }

    private Set<Authority> calculateRoles(final Set<String> additionalRoles) {
        final var additionalRolesWithRolePrefix = additionalRoles.stream().map(Authority::addRolePrefixIfNecessary).collect(Collectors.toSet());
        return Sets.intersection(ALLOWED_AUTH_USER_ROLES.keySet(), additionalRolesWithRolePrefix).stream().map(Authority::new).collect(Collectors.toSet());
    }

    private void validate(final String username, final String email, final String firstName, final String lastName)
            throws CreateUserException, VerifyEmailException {

        if (StringUtils.length(username) < 6) {
            throw new CreateUserException("username", "length");
        }
        if (!username.matches("^[A-Z0-9_]*$")) {
            throw new CreateUserException("username", "format");
        }
        if (StringUtils.length(firstName) < 2) {
            throw new CreateUserException("firstName", "length");
        }
        if (StringUtils.length(lastName) < 2) {
            throw new CreateUserException("lastName", "length");
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

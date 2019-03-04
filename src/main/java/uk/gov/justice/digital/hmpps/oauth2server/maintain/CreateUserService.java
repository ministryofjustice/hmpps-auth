package uk.gov.justice.digital.hmpps.oauth2server.maintain;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreateUserService {
    private static final List<String> LICENCES_ROLES = List.of("ROLE_LICENCE_RO", "ROLE_GLOBAL_SEARCH");

    private final UserTokenRepository userTokenRepository;
    private final UserEmailRepository userEmailRepository;
    private final NotificationClientApi notificationClient;
    private final TelemetryClient telemetryClient;
    private final VerifyEmailService verifyEmailService;
    private final String licencesTemplateId;

    public CreateUserService(final UserTokenRepository userTokenRepository,
                             final UserEmailRepository userEmailRepository,
                             final NotificationClientApi notificationClient,
                             final TelemetryClient telemetryClient,
                             final VerifyEmailService verifyEmailService, @Value("${application.notify.reset.template}") final String licencesTemplateId) {
        this.userTokenRepository = userTokenRepository;
        this.userEmailRepository = userEmailRepository;
        this.notificationClient = notificationClient;
        this.telemetryClient = telemetryClient;
        this.verifyEmailService = verifyEmailService;
        this.licencesTemplateId = licencesTemplateId;
    }

    @Transactional
    public String createUser(final String usernameInput, final String email, final String firstName, final String lastName, final String url)
            throws CreateUserException, NotificationClientException, VerifyEmailException {
        // ensure username always uppercase
        final var username = StringUtils.upperCase(usernameInput);

        // validate
        validate(username, email, firstName, lastName);

        // create the user
        final var person = new Person(username, firstName, lastName);
        final var authorities = LICENCES_ROLES.stream().map(Authority::new).collect(Collectors.toSet());
        final var user = new UserEmail(username, null, email, false, false, true, true, LocalDateTime.now(), person, authorities);
        userEmailRepository.save(user);

        // then the reset token
        final var userToken = new UserToken(TokenType.RESET, user);
        userTokenRepository.save(userToken);

        final var setPasswordLink = url + userToken.getToken();
        final var parameters = Map.of("firstName", firstName, "resetLink", setPasswordLink);

        // send the email
        try {
            log.info("Sending initial set password to notify for user {}", username);
            notificationClient.sendEmail(licencesTemplateId, email, parameters, null);
            telemetryClient.trackEvent("CreateUserSuccess", Map.of("username", username), null);
        } catch (final NotificationClientException e) {
            final var reason = (e.getCause() != null ? e.getCause() : e).getClass().getSimpleName();
            log.warn("Failed to send create user notify for user {}", username, e);
            telemetryClient.trackEvent("CreateUserFailure", Map.of("username", username, "reason", reason), null);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                notificationClient.sendEmail(licencesTemplateId, email, parameters, null, null);
            }
            throw e;
        }

        // return the reset link to the controller
        return setPasswordLink;
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
}

package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class VerifyEmailService {

    private static final String EXISTING_EMAIL_SQL = "select distinct internet_address from internet_addresses i " +
            "inner join STAFF_USER_ACCOUNTS s on i.owner_id = s.staff_id and owner_class = 'STF' " +
            "where internet_address_class = 'EMAIL' and s.username = ?";
    private final UserEmailRepository userEmailRepository;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;
    private final TelemetryClient telemetryClient;
    private final UsernameTokenHelper usernameTokenHelper = new UsernameTokenHelper();
    private final NotificationClientApi notificationClient;
    private final String notifyTemplateId;

    public VerifyEmailService(final UserEmailRepository userEmailRepository,
                              final UserService userService, final JdbcTemplate jdbcTemplate,
                              final TelemetryClient telemetryClient,
                              final NotificationClientApi notificationClient,
                              @Value("${application.notify.verify.template}") final String notifyTemplateId) {
        this.userEmailRepository = userEmailRepository;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
        this.telemetryClient = telemetryClient;
        this.notificationClient = notificationClient;
        this.notifyTemplateId = notifyTemplateId;
    }

    public Optional<UserEmail> getEmail(final String username) {
        return userEmailRepository.findById(username);
    }

    public boolean isNotVerified(final String name) {
        return !getEmail(name).map(UserEmail::isVerified).orElse(Boolean.FALSE);
    }

    public List<String> getExistingEmailAddresses(final String username) {
        return jdbcTemplate.queryForList(EXISTING_EMAIL_SQL, String.class, username);
    }

    public String requestVerification(final String username, final String email, final String url) throws NotificationClientException {
        final var user = userService.getUserByUsername(username);
        final var firstName = user.map(u -> u.getStaff().getFirstName()).orElse(username);
        final var optionalUserEmail = userEmailRepository.findById(username);
        final var userEmail = optionalUserEmail.orElseGet(() -> new UserEmail(username));
        userEmail.setEmail(email);
        userEmail.setVerificationToken(UUID.randomUUID().toString());
        userEmail.setTokenExpiry(LocalDateTime.now().plusDays(1));

        final var jwt = usernameTokenHelper.createUsernameTokenEncodedString(username, userEmail.getVerificationToken());
        final var verifyLink = String.format("%s/%s", url, jwt);
        final var parameters = Map.of("firstName", firstName, "verifyLink", verifyLink);

        try {
            log.info("Sending email verification to notify for user {}", username);
            notificationClient.sendEmail(notifyTemplateId, email, parameters, null);
            telemetryClient.trackEvent("VerifyEmailRequestSuccess", Map.of("username", username), null);
        } catch (final NotificationClientException e) {
            final var reason = (e.getCause() != null ? e.getCause() : e).getClass().getSimpleName();
            log.warn("Failed to send email verification to notify for user {}", username, e);
            telemetryClient.trackEvent("VerifyEmailRequestFailure", Map.of("username", username, "reason", reason), null);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                notificationClient.sendEmail(notifyTemplateId, email, parameters, null, null);
            }
            throw e;
        }

        userEmailRepository.save(userEmail);

        return verifyLink;
    }

    public Optional<String> confirmEmail(final String token) {
        final var usernameTokenOptional = usernameTokenHelper.readUsernameTokenFromEncodedString(token);
        if (usernameTokenOptional.isEmpty()) {
            return trackAndReturnFailureForInvalidToken();
        }
        final var username = usernameTokenOptional.get().getUsername();
        final var verificationToken = usernameTokenOptional.get().getToken();

        final var userEmailOptional = userEmailRepository.findById(username);
        if (userEmailOptional.isEmpty()) {
            return trackAndReturnFailureForInvalidToken(username, "notfound");
        }

        final var userEmail = userEmailOptional.get();
        if (userEmail.isVerified()) {
            log.info("Verify email succeeded due to already verified");
            telemetryClient.trackEvent("VerifyEmailConfirmFailure", Map.of("reason", "alreadyverified", "username", username), null);
            return Optional.empty();
        }
        if (!verificationToken.equals(userEmail.getVerificationToken())) {
            return trackAndReturnFailureForInvalidToken(username, "tokenMismatch");
        }
        if (userEmail.getTokenExpiry().isBefore(LocalDateTime.now())) {
            return trackAndReturnFailureForInvalidToken(username, "expired");
        }

        markEmailAsVerified(userEmail);
        return Optional.empty();
    }

    private void markEmailAsVerified(final UserEmail userEmail) {
        // verification token match
        userEmail.setVerified(true);
        // and clear token
        userEmail.setVerificationToken(null);
        userEmail.setTokenExpiry(null);

        userEmailRepository.save(userEmail);

        log.info("Verify email succeeded for {}", userEmail.getUsername());
        telemetryClient.trackEvent("VerifyEmailConfirmSuccess", Map.of("username", userEmail.getUsername()), null);
    }

    private Optional<String> trackAndReturnFailureForInvalidToken() {
        log.info("Verify email failed due to invalid token");
        telemetryClient.trackEvent("VerifyEmailConfirmFailure", Map.of("reason", "invalid"), null);
        return Optional.of("invalid");
    }

    private Optional<String> trackAndReturnFailureForInvalidToken(final String username, final String reason) {
        log.info("Verify email failed due to {}", reason);
        telemetryClient.trackEvent("VerifyEmailConfirmFailure", Map.of("reason", reason, "username", username), null);
        return Optional.of(reason);
    }
}

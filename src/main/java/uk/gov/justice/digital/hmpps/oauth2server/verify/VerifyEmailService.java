package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class VerifyEmailService {

    private static final String EXISTING_EMAIL_SQL = "select distinct internet_address from internet_addresses i " +
            "inner join STAFF_USER_ACCOUNTS s on i.owner_id = s.staff_id and owner_class = 'STF' " +
            "where internet_address_class = 'EMAIL' and s.username = ?";
    private final UserEmailRepository userEmailRepository;
    private final UserTokenRepository userTokenRepository;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;
    private final TelemetryClient telemetryClient;
    private final NotificationClientApi notificationClient;
    private final String notifyTemplateId;

    public VerifyEmailService(final UserEmailRepository userEmailRepository,
                              final UserTokenRepository userTokenRepository,
                              final UserService userService, final JdbcTemplate jdbcTemplate,
                              final TelemetryClient telemetryClient,
                              final NotificationClientApi notificationClient,
                              @Value("${application.notify.verify.template}") final String notifyTemplateId) {
        this.userEmailRepository = userEmailRepository;
        this.userTokenRepository = userTokenRepository;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
        this.telemetryClient = telemetryClient;
        this.notificationClient = notificationClient;
        this.notifyTemplateId = notifyTemplateId;
    }

    public Optional<UserEmail> getEmail(final String username) {
        return userEmailRepository.findById(username).filter(ue -> StringUtils.isNotBlank(ue.getEmail()));
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

        // check for an existing token and delete as we now have a new one
        final var existingTokenOptional = userTokenRepository.findByTokenTypeAndUserEmail(TokenType.VERIFIED, userEmail);
        existingTokenOptional.ifPresent(userTokenRepository::delete);

        final var userToken = new UserToken(TokenType.VERIFIED, userEmail);
        final var verifyLink = url + userToken.getToken();
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
        userTokenRepository.save(userToken);

        return verifyLink;
    }

    public Optional<String> confirmEmail(final String token) {
        final var userTokenOptional = userTokenRepository.findById(token);
        if (userTokenOptional.isEmpty()) {
            return trackAndReturnFailureForInvalidToken();
        }
        final var userToken = userTokenOptional.get();
        final var userEmail = userToken.getUserEmail();
        final var username = userEmail.getUsername();

        if (userEmail.isVerified()) {
            log.info("Verify email succeeded due to already verified");
            telemetryClient.trackEvent("VerifyEmailConfirmFailure", Map.of("reason", "alreadyverified", "username", username), null);
            return Optional.empty();
        }
        if (userToken.hasTokenExpired()) {
            return trackAndReturnFailureForExpiredToken(username);
        }

        markEmailAsVerified(userEmail);
        return Optional.empty();
    }

    private void markEmailAsVerified(final UserEmail userEmail) {
        // verification token match
        userEmail.setVerified(true);
        userEmailRepository.save(userEmail);

        log.info("Verify email succeeded for {}", userEmail.getUsername());
        telemetryClient.trackEvent("VerifyEmailConfirmSuccess", Map.of("username", userEmail.getUsername()), null);
    }

    private Optional<String> trackAndReturnFailureForInvalidToken() {
        log.info("Verify email failed due to invalid token");
        telemetryClient.trackEvent("VerifyEmailConfirmFailure", Map.of("reason", "invalid"), null);
        return Optional.of("invalid");
    }

    private Optional<String> trackAndReturnFailureForExpiredToken(final String username) {
        log.info("Verify email failed due to expired token");
        telemetryClient.trackEvent("VerifyEmailConfirmFailure", Map.of("reason", "expired", "username", username), null);
        return Optional.of("expired");
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.utils.EmailHelper;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
public class VerifyEmailService {

    @SuppressWarnings("SqlResolve")
    private static final String EXISTING_EMAIL_SQL = "select distinct internet_address from internet_addresses i " +
            "inner join STAFF_USER_ACCOUNTS s on i.owner_id = s.staff_id and owner_class = 'STF' " +
            "where internet_address_class = 'EMAIL' and s.username = ?";
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TelemetryClient telemetryClient;
    private final NotificationClientApi notificationClient;
    private final ReferenceCodesService referenceCodesService;
    private final String notifyTemplateId;

    public VerifyEmailService(final UserRepository userRepository,
                              final UserTokenRepository userTokenRepository,
                              final JdbcTemplate jdbcTemplate,
                              final TelemetryClient telemetryClient,
                              final NotificationClientApi notificationClient,
                              final ReferenceCodesService referenceCodesService, @Value("${application.notify.verify.template}") final String notifyTemplateId) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.telemetryClient = telemetryClient;
        this.notificationClient = notificationClient;
        this.referenceCodesService = referenceCodesService;
        this.notifyTemplateId = notifyTemplateId;
    }

    public Optional<User> getEmail(final String username) {
        return userRepository.findByUsername(username).filter(ue -> StringUtils.isNotBlank(ue.getEmail()));
    }

    public boolean isNotVerified(final String name) {
        return !getEmail(name).map(User::isVerified).orElse(Boolean.FALSE);
    }

    public List<String> getExistingEmailAddresses(final String username) {
        return jdbcTemplate.queryForList(EXISTING_EMAIL_SQL, String.class, username);
    }

    @Transactional(transactionManager = "authTransactionManager")
    public String requestVerification(final String username, final String emailInput, final String firstName, final String url, final EmailType emailType) throws NotificationClientException, VerifyEmailException {
        final var user = userRepository.findByUsername(username).orElseThrow();
        final var verifyLink = url + user.createToken(TokenType.VERIFIED).getToken();
        final var parameters = Map.of("firstName", firstName, "verifyLink", verifyLink);

        final var email = EmailHelper.format(emailInput);
        validateEmailAddress(email, emailType);

        switch (emailType) {
            case PRIMARY:
                user.setEmail(email);
                user.setVerified(false);
                break;
            case SECONDARY:
                user.addContact(ContactType.SECONDARY_EMAIL, email);
                break;
            default:
                log.warn("Failed to send email verification to notify for user {} invalid emailType Enum", username);
                telemetryClient.trackEvent("VerifyEmailRequestFailure", Map.of("username", username, "reason", "invalid emailType Enum"), null);
                throw new RuntimeException("invalid emailType Enum");
        }

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

        userRepository.save(user);

        return verifyLink;
    }

    @Transactional(transactionManager = "authTransactionManager")
    public Optional<String> resendVerificationCodeSecondaryEmail(final String username, final String url) throws NotificationClientException, VerifyEmailException {
        final var user = userRepository.findByUsername(username).orElseThrow();
        if (user.getSecondaryEmail() == null) {
            throw new VerifyEmailException("nosecondaryemail");
        }
        if (user.isSecondaryEmailVerified()) {
            log.info("Verify secondary email succeeded due to already verified");
            telemetryClient.trackEvent("VerifySecondaryEmailConfirmFailure", Map.of("reason", "alreadyverified", "username", username), null);
            return Optional.empty();
        }

        final var verifyLink = url + user.createToken(TokenType.VERIFIED).getToken();
        final var parameters = Map.of("firstName", user.getFirstName(), "verifyLink", verifyLink);
        notificationClient.sendEmail(notifyTemplateId, user.getSecondaryEmail(), parameters, null);

        return Optional.of(verifyLink);
    }

    public boolean secondaryEmailVerified(final String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", username)))
                .isSecondaryEmailVerified();
    }

    public void validateEmailAddress(final String email, final EmailType emailType) throws VerifyEmailException {
        validateEmailAddressExcludingGsi(email, emailType);
        if (email.matches(".*@.*\\.gsi\\.gov\\.uk")) throw new VerifyEmailException("gsi");
    }

    public void validateEmailAddressExcludingGsi(final String email, final EmailType emailType) throws VerifyEmailException {
        if (StringUtils.isBlank(email)) {
            throw new VerifyEmailException("blank");
        }

        final var atIndex = StringUtils.indexOf(email, '@');
        if (atIndex == -1 || !email.matches(".*@.*\\..*")) {
            throw new VerifyEmailException("format");
        }
        final var firstCharacter = email.charAt(0);
        final var lastCharacter = email.charAt(email.length() - 1);
        if (firstCharacter == '.' || firstCharacter == '@' ||
                lastCharacter == '.' || lastCharacter == '@') {
            throw new VerifyEmailException("firstlast");
        }
        if (email.matches(".*\\.@.*") || email.matches(".*@\\..*")) {
            throw new VerifyEmailException("together");
        }
        if (StringUtils.countMatches(email, '@') > 1) {
            throw new VerifyEmailException("at");
        }
        if (StringUtils.containsWhitespace(email)) {
            throw new VerifyEmailException("white");
        }
        if (!email.matches("[0-9A-Za-z@.'_\\-+]*")) {
            throw new VerifyEmailException("characters");
        }
        if (emailType == EmailType.PRIMARY && !referenceCodesService.isValidEmailDomain(email.substring(atIndex + 1))) {
            throw new VerifyEmailException("domain");
        }
    }

    @Transactional(transactionManager = "authTransactionManager")
    public Optional<String> confirmEmail(final String token) {
        final var userTokenOptional = userTokenRepository.findById(token);
        if (userTokenOptional.isEmpty()) {
            return trackAndReturnFailureForInvalidToken();
        }
        final var userToken = userTokenOptional.get();
        final var user = userToken.getUser();
        final var username = user.getUsername();

        if (user.isVerified()) {
            log.info("Verify email succeeded due to already verified");
            telemetryClient.trackEvent("VerifyEmailConfirmFailure", Map.of("reason", "alreadyverified", "username", username), null);
            return Optional.empty();
        }
        if (userToken.hasTokenExpired()) {
            return trackAndReturnFailureForExpiredToken(username);
        }

        markEmailAsVerified(user);
        return Optional.empty();
    }

    @Transactional(transactionManager = "authTransactionManager")
    public Optional<String> confirmSecondaryEmail(final String token) {
        final var userTokenOptional = userTokenRepository.findById(token);
        if (userTokenOptional.isEmpty()) {
            return trackAndReturnFailureForInvalidToken();
        }
        final var userToken = userTokenOptional.get();
        final var user = userToken.getUser();
        final var username = user.getUsername();

        if (user.isSecondaryEmailVerified()) {
            log.info("Verify secondary email succeeded due to already verified");
            telemetryClient.trackEvent("VerifySecondayEmailConfirmFailure", Map.of("reason", "alreadyverified", "username", username), null);
            return Optional.empty();
        }
        if (userToken.hasTokenExpired()) {
            return trackAndReturnFailureForExpiredToken(username);
        }

        markSecondaryEmailAsVerified(user);
        return Optional.empty();
    }

    private void markEmailAsVerified(final User user) {
        // verification token match
        user.setVerified(true);
        userRepository.save(user);

        log.info("Verify email succeeded for {}", user.getUsername());
        telemetryClient.trackEvent("VerifyEmailConfirmSuccess", Map.of("username", user.getUsername()), null);
    }

    private void markSecondaryEmailAsVerified(final User user) {
        // verification token match
        user.findContact(ContactType.SECONDARY_EMAIL).ifPresent(c -> c.setVerified(true));
        userRepository.save(user);

        log.info("Verify secondary email succeeded for {}", user.getUsername());
        telemetryClient.trackEvent("VerifySecondaryEmailConfirmSuccess", Map.of("username", user.getUsername()), null);
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

    public static class VerifyEmailException extends Exception {
        private final String reason;

        public VerifyEmailException(final String reason) {
            super(String.format("Verify email failed with reason: %s", reason));
            this.reason = reason;
        }

        public String getReason() {
            return this.reason;
        }
    }

}

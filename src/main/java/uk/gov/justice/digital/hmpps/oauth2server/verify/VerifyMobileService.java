package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
public class VerifyMobileService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final TelemetryClient telemetryClient;
    private final NotificationClientApi notificationClient;
    private final String notifyTemplateId;

    public VerifyMobileService(final UserRepository userRepository,
                               final UserTokenRepository userTokenRepository,
                               final TelemetryClient telemetryClient,
                               final NotificationClientApi notificationClient,
                               @Value("${application.notify.verify-mobile.template}") final String notifyTemplateId) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.telemetryClient = telemetryClient;
        this.notificationClient = notificationClient;
        this.notifyTemplateId = notifyTemplateId;
    }

    public Optional<User> getMobile(final String username) {
        return userRepository.findByUsername(username).filter(um -> StringUtils.isNotBlank(um.getMobile()));
    }

    public boolean isNotVerified(final String name) {
        return !getMobile(name).map(User::isVerified).orElse(Boolean.FALSE);
    }


    @Transactional(transactionManager = "authTransactionManager")
    public String requestVerification(final String username, final String mobile) throws VerifyMobileException, NotificationClientException {
        final var user = userRepository.findByUsername(username).orElseThrow();
        final var verifyCode = user.createToken(TokenType.MOBILE).getToken();
        final var parameters = Map.of("verifyCode", verifyCode);
        validateMobileNumber(mobile);
        final var formattedMobile = mobile.replaceAll("\\s+", "");
        user.setMobile(formattedMobile);
        user.setMobileVerified(false);
        sendNotification(username, formattedMobile, parameters);

        userRepository.save(user);

        return verifyCode;
    }

    private void sendNotification(final String username, final String mobile, final Map<String, String> parameters) throws NotificationClientException {
        try {
            log.info("Sending sms verification to notify for user {}", username);
            notificationClient.sendSms(notifyTemplateId, mobile, parameters, null);
            telemetryClient.trackEvent("VerifyMobileRequestSuccess", Map.of("username", username), null);
        } catch (final NotificationClientException e) {
            final var reason = (e.getCause() != null ? e.getCause() : e).getClass().getSimpleName();
            log.warn("Failed to send sms verification to notify for user {}", username, e);
            telemetryClient.trackEvent("VerifyMobileRequestFailure", Map.of("username", username, "reason", reason), null);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                notificationClient.sendSms(notifyTemplateId, mobile, parameters, null, null);
            }
            throw e;
        }
    }

    public void validateMobileNumber(final String mobile) throws VerifyMobileException {
        if (StringUtils.isBlank(mobile)) {
            throw new VerifyMobileException("blank");
        }
        if (!mobile.matches("((\\+44(\\s\\s|\\s0\\s|\\s)?)|0)7\\d{3}(\\s)?\\d{6}")) {
            throw new VerifyMobileException("format");
        }
    }

    @Transactional(transactionManager = "authTransactionManager")
    public Optional<Map<String, String>> confirmMobile(final String token) throws NotificationClientException {
        final var userTokenOptional = userTokenRepository.findById(token);
        if (userTokenOptional.isEmpty()) {
            return trackAndReturnFailureForInvalidToken();
        }
        final var userToken = userTokenOptional.get();
        final var user = userToken.getUser();
        final var username = user.getUsername();

        if (user.isMobileVerified()) {
            log.info("Verify mobile succeeded due to already verified");
            telemetryClient.trackEvent("VerifyMobileConfirmFailure", Map.of("reason", "alreadyverified", "username", username), null);
            return Optional.empty();
        }
        if (userToken.hasTokenExpired()) {
            return issueNewTokenToReplaceExpiredToken(username);
        }

        markMobileAsVerified(user);
        return Optional.empty();
    }

    private void markMobileAsVerified(final User user) {
        // verification token match
        user.setMobileVerified(true);
        userRepository.save(user);

        log.info("Verify mobile succeeded for {}", user.getUsername());
        telemetryClient.trackEvent("VerifyMobileConfirmSuccess", Map.of("username", user.getUsername()), null);
    }

    private Optional<Map<String, String>> trackAndReturnFailureForInvalidToken() {
        log.info("Verify mobile failed due to invalid token");
        telemetryClient.trackEvent("VerifyMobileConfirmFailure", Map.of("error", "invalid"), null);
        return Optional.of(Map.of("error", "invalid"));
    }

    private Optional<Map<String, String>> issueNewTokenToReplaceExpiredToken(final String username) throws NotificationClientException {
        log.info("Verify mobile failed due to expired token");
        telemetryClient.trackEvent("VerifyMobileConfirmFailure", Map.of("reason", "expired", "username", username), null);
        final var user = userRepository.findByUsername(username).orElseThrow();
        final var verifyCode = user.createToken(TokenType.MOBILE).getToken();
        final var parameters = Map.of("verifyCode", verifyCode);
        sendNotification(username, user.getMobile(), parameters);
        return Optional.of(Map.of("error", "expired", "verifyCode", verifyCode));
    }

    @Transactional(transactionManager = "authTransactionManager")
    public Optional<String> resendVerificationCode(final String username) throws NotificationClientException, VerifyMobileException {
        final var user = userRepository.findByUsername(username).orElseThrow();
        if (user.getMobile() == null) {
            throw new VerifyMobileException("nomobile");
        }
        if (user.isMobileVerified()) {
            log.info("Verify mobile succeeded due to already verified");
            telemetryClient.trackEvent("VerifyMobileConfirmFailure", Map.of("reason", "alreadyverified", "username", username), null);
            return Optional.empty();
        }

        final var verifyCode = user.createToken(TokenType.MOBILE).getToken();
        final var parameters = Map.of("verifyCode", verifyCode);
        sendNotification(username, user.getMobile(), parameters);

        return Optional.of(verifyCode);
    }

    public boolean mobileVerified(final String username) {
        return userRepository.findByUsername(username).orElseThrow().isMobileVerified();
    }


    @Getter
    public static class VerifyMobileException extends Exception {
        private final String reason;

        public VerifyMobileException(final String reason) {
            super(String.format("Verify mobile failed with reason: %s", reason));
            this.reason = reason;
        }
    }

}

package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class ResetPasswordService {

    private final UserEmailRepository userEmailRepository;
    private final UserTokenRepository userTokenRepository;
    private final UserService userService;
    private final ChangePasswordService changePasswordService;
    private final TelemetryClient telemetryClient;
    private final NotificationClientApi notificationClient;
    private final String resetTemplateId;
    private final String resetUnavailableTemplateId;

    public ResetPasswordService(final UserEmailRepository userEmailRepository,
                                final UserTokenRepository userTokenRepository, final UserService userService,
                                final ChangePasswordService changePasswordService, final TelemetryClient telemetryClient,
                                final NotificationClientApi notificationClient,
                                @Value("${application.notify.reset.template}") final String resetTemplateId,
                                @Value("${application.notify.reset-unavailable.template}") final String resetUnavailableTemplateId
    ) {
        this.userEmailRepository = userEmailRepository;
        this.userTokenRepository = userTokenRepository;
        this.userService = userService;
        this.changePasswordService = changePasswordService;
        this.telemetryClient = telemetryClient;
        this.notificationClient = notificationClient;
        this.resetTemplateId = resetTemplateId;
        this.resetUnavailableTemplateId = resetUnavailableTemplateId;
    }

    public Optional<String> requestResetPassword(final String username, final String url) throws NotificationClientException {
        final var optionalUserEmail = userEmailRepository.findById(username);

        if (optionalUserEmail.isEmpty() || StringUtils.isEmpty(optionalUserEmail.get().getEmail())) {
            // no user found or email address found, so nothing more we can do.  Bail
            return Optional.empty();
        }

        final var userOptional = userService.getUserByUsername(username);
        final var userEmail = optionalUserEmail.get();

        final String emailTemplate;
        final Map<String, String> parameters;
        if (userOptional.isEmpty()) {
            emailTemplate = resetUnavailableTemplateId;
            parameters = Map.of("firstName", username);
        } else {
            final var staffUserAccount = userOptional.get();
            final var status = staffUserAccount.getAccountDetail().getStatus();
            // only allow reset for active accounts that aren't locked
            // or are locked by getting password incorrect (in either c-nomis or auth)
            final var firstName = staffUserAccount.getStaff().getFirstName();
            if (staffUserAccount.getStaff().isActive() && (!status.isLocked() || status.isUserLocked() || userEmail.isLocked())) {
                emailTemplate = resetTemplateId;
                final var userTokenOptional = userTokenRepository.findByTokenTypeAndUserEmail(TokenType.RESET, userEmail);
                // delete any existing token
                userTokenOptional.ifPresent(userTokenRepository::delete);

                final var userToken = new UserToken(TokenType.RESET, userEmail);
                userTokenRepository.save(userToken);

                final var resetLink = String.format("%s/%s", url, userToken.getToken());
                parameters = Map.of("firstName", firstName, "resetLink", resetLink);
            } else {
                emailTemplate = resetUnavailableTemplateId;
                parameters = Map.of("firstName", firstName);
            }
        }

        final var email = userEmail.getEmail();
        try {
            log.info("Sending reset password to notify for user {}", username);
            notificationClient.sendEmail(emailTemplate, email, parameters, null);
            telemetryClient.trackEvent("ResetPasswordRequestSuccess", Map.of("username", username), null);
        } catch (final NotificationClientException e) {
            final var reason = (e.getCause() != null ? e.getCause() : e).getClass().getSimpleName();
            log.warn("Failed to send reset password to notify for user {}", username, e);
            telemetryClient.trackEvent("ResetPasswordRequestFailure", Map.of("username", username, "reason", reason), null);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                notificationClient.sendEmail(emailTemplate, email, parameters, null, null);
            }
            throw e;
        }

        return Optional.ofNullable(parameters.get("resetLink"));
    }

    public Optional<UserToken> getToken(final String token) {
        final var userTokenOptional = userTokenRepository.findById(token);
        return userTokenOptional.filter(t -> t.getTokenType() == TokenType.RESET);
    }

    public Optional<String> checkToken(final String token) {
        final var userTokenOptional = getToken(token);
        if (userTokenOptional.isEmpty()) {
            log.info("Failed to reset password due to invalid token");
            telemetryClient.trackEvent("ResetPasswordFailure", Map.of("reason", "invalid"), null);
            return Optional.of("invalid");
        }
        final var userToken = userTokenOptional.get();
        final var username = userToken.getUserEmail().getUsername();
        if (userToken.hasTokenExpired()) {
            log.info("Failed to reset password due to expired token");
            telemetryClient.trackEvent("ResetPasswordFailure", Map.of("username", username, "reason", "expired"), null);
            return Optional.of("expired");
        }
        return Optional.empty();
    }

    public void resetPassword(final String token, final String newPassword) {
        final var userToken = userTokenRepository.findById(token).orElseThrow();
        final var userEmail = userToken.getUserEmail();
        userEmail.setLocked(false);
        changePasswordService.changePasswordWithUnlock(userEmail.getUsername(), newPassword);
        userEmailRepository.save(userEmail);
        userTokenRepository.delete(userToken);
    }
}

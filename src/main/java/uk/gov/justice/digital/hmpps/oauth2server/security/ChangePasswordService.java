package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.verify.PasswordServiceImpl;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class ChangePasswordService extends PasswordServiceImpl {
    private final UserTokenRepository userTokenRepository;
    private final UserEmailRepository userEmailRepository;
    private final UserService userService;
    private final AlterUserService alterUserService;
    private final TelemetryClient telemetryClient;
    private final NotificationClientApi notificationClient;
    private final String resetPasswordTemplateId;

    protected ChangePasswordService(final UserTokenRepository userTokenRepository,
                                    final UserEmailRepository userEmailRepository,
                                    final UserService userService, final AlterUserService alterUserService,
                                    final PasswordEncoder passwordEncoder,
                                    final TelemetryClient telemetryClient,
                                    final NotificationClientApi notificationClient,
                                    @Value("${application.authentication.password-age}") final long passwordAge,
                                    @Value("${application.notify.reset-password.template}") String resetPasswordTemplateId) {
        super(passwordEncoder, passwordAge);
        this.userTokenRepository = userTokenRepository;
        this.userEmailRepository = userEmailRepository;
        this.userService = userService;
        this.alterUserService = alterUserService;
        this.telemetryClient = telemetryClient;
        this.notificationClient = notificationClient;
        this.resetPasswordTemplateId = resetPasswordTemplateId;
    }

    @Transactional(transactionManager = "authTransactionManager")
    String createToken(final String username) {
        final var userEmailOptional = userEmailRepository.findById(username);
        final var userEmail = userEmailOptional.orElseGet(() -> {
            final var ue = new UserEmail(username);
            userEmailRepository.save(ue);
            return ue;
        });
        final var userTokenOptional = userTokenRepository.findByTokenTypeAndUserEmail(TokenType.CHANGE, userEmail);
        userTokenOptional.ifPresent(userTokenRepository::delete);

        final var userToken = new UserToken(TokenType.CHANGE, userEmail);
        userTokenRepository.save(userToken);
        log.info("Requesting change password for {}", username);
        telemetryClient.trackEvent("ChangePasswordRequest", Map.of("username", username), null);
        return userToken.getToken();
    }

    @Override
    public void setPassword(final String token, final String password) {
        final var userToken = userTokenRepository.findById(token).orElseThrow();
        final var userEmail = userToken.getUserEmail();
        final var userOptional = userEmail.isMaster() ? Optional.of(userEmail) : userService.findUser(userEmail.getUsername());

        // before we set, ensure user allowed to still change their password
        if (userOptional.map(u -> !u.isEnabled() || !u.isAccountNonLocked()).orElse(Boolean.TRUE)) {
            // failed, so let user know
            throw new LockedException("locked");
        }

        // if we're the master of this user record deal with the change of password here
        if (userEmail.isMaster()) {
            changeAuthPassword(userEmail, password);
        } else {
            alterUserService.changePassword(userEmail.getUsername(), password);
        }
        userEmailRepository.save(userEmail);
        userTokenRepository.delete(userToken);
        sendPasswordResetEmail(userEmail);
    }

    private void sendPasswordResetEmail(final UserEmail userEmail) {
        // then the reset token
        final var username = userEmail.getUsername();
        final var email = userEmail.getEmail();
        final var parameters = Map.of("firstName", userEmail.getFirstName(), "username", username);

        // send the email
        try {
            log.info("Sending password reset to notify for user {}", username);
            notificationClient.sendEmail(resetPasswordTemplateId, email, parameters, null);
        } catch (final NotificationClientException e) {
            final var reason = (e.getCause() != null ? e.getCause() : e).getClass().getSimpleName();
            log.warn("Failed to send password reset notify for user {}", username, e);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                try {
                    notificationClient.sendEmail(resetPasswordTemplateId, email, parameters, null, null);
                } catch (NotificationClientException ex) {
                    log.error("Failed to send password reset notify for user {}", username, ex);
                }
            }
        }
    }
}

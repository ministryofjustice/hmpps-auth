package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.service.notify.NotificationClientApi;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class TokenService {

    private final UserEmailRepository userEmailRepository;
    private final UserTokenRepository userTokenRepository;
    private final UserService userService;
    private final ChangePasswordService changePasswordService;
    private final TelemetryClient telemetryClient;
    private final NotificationClientApi notificationClient;
    private final String resetTemplateId;
    private final String resetUnavailableTemplateId;

    public TokenService(final UserEmailRepository userEmailRepository,
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

    public Optional<UserToken> getToken(final TokenType tokenType, final String token) {
        final var userTokenOptional = userTokenRepository.findById(token);
        return userTokenOptional.filter(t -> t.getTokenType() == tokenType);
    }

    public Optional<String> checkToken(final TokenType tokenType, final String token) {
        final var userTokenOptional = getToken(tokenType, token);
        if (userTokenOptional.isEmpty()) {
            log.info("Failed to {} password due to invalid token", tokenType.getDescription());
            telemetryClient.trackEvent(String.format("%sPasswordFailure", tokenType.getDescription()),
                    Map.of("reason", "invalid"), null);
            return Optional.of("invalid");
        }
        final var userToken = userTokenOptional.get();
        final var username = userToken.getUserEmail().getUsername();
        if (userToken.hasTokenExpired()) {
            log.info("Failed to reset password due to expired token");
            telemetryClient.trackEvent(String.format("%sPasswordFailure", tokenType.getDescription()),
                    Map.of("username", username, "reason", "expired"), null);
            return Optional.of("expired");
        }
        return Optional.empty();
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
public class TokenService {

    private final UserTokenRepository userTokenRepository;
    private final TelemetryClient telemetryClient;

    public TokenService(final UserTokenRepository userTokenRepository,
                        final TelemetryClient telemetryClient) {
        this.userTokenRepository = userTokenRepository;
        this.telemetryClient = telemetryClient;
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
        final var username = userToken.getUser().getUsername();
        if (userToken.hasTokenExpired()) {
            log.info("Failed to reset password due to expired token");
            telemetryClient.trackEvent(String.format("%sPasswordFailure", tokenType.getDescription()),
                    Map.of("username", username, "reason", "expired"), null);
            return Optional.of("expired");
        }
        return Optional.empty();
    }
}

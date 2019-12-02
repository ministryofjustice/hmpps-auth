package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.PasswordService;

import java.util.Map;

@Service
@Slf4j
@Transactional
@AllArgsConstructor
public class ChangePasswordService implements PasswordService {
    private final UserTokenRepository userTokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final DelegatingUserService delegatingUserService;
    private final TelemetryClient telemetryClient;

    @Transactional(transactionManager = "authTransactionManager")
    public String createToken(final String username) {
        final var userOptional = userRepository.findByUsername(username);
        final var user = userOptional.orElseGet(() -> {
            final var userPersonDetails = userService.findMasterUserPersonDetails(username).orElseThrow();
            final var ue = userPersonDetails.toUser();
            return userRepository.save(ue);
        });
        final var userToken = user.createToken(TokenType.CHANGE);
        log.info("Requesting change password for {}", username);
        telemetryClient.trackEvent("ChangePasswordRequest", Map.of("username", username), null);
        return userToken.getToken();
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager")
    public void setPassword(final String token, final String password) {
        final var userToken = userTokenRepository.findById(token).orElseThrow();
        final var user = userToken.getUser();
        final var userPersonDetails = user.isMaster() ? user : userService.findMasterUserPersonDetails(user.getUsername()).orElseThrow();

        // before we set, ensure user allowed to still change their password
        if (!userPersonDetails.isEnabled() || !userPersonDetails.isAccountNonLocked()) {
            // failed, so let user know
            throw new LockedException("locked");
        }

        delegatingUserService.changePassword(userPersonDetails, password);

        user.removeToken(userToken);
        userRepository.save(user);
    }

}

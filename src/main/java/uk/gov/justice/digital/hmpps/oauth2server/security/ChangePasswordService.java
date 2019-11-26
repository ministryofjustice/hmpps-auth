package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.verify.PasswordServiceImpl;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class ChangePasswordService extends PasswordServiceImpl {
    private final UserTokenRepository userTokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AlterUserService alterUserService;
    private final TelemetryClient telemetryClient;

    protected ChangePasswordService(final UserTokenRepository userTokenRepository,
                                    final UserRepository userRepository,
                                    final UserService userService, final AlterUserService alterUserService,
                                    final PasswordEncoder passwordEncoder,
                                    final TelemetryClient telemetryClient,
                                    @Value("${application.authentication.password-age}") final long passwordAge) {
        super(passwordEncoder, passwordAge);
        this.userTokenRepository = userTokenRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.alterUserService = alterUserService;
        this.telemetryClient = telemetryClient;
    }

    @Transactional(transactionManager = "authTransactionManager")
    public String createToken(final String username) {
        final var userOptional = userRepository.findByUsername(username);
        final var user = userOptional.orElseGet(() -> {
            final var ue = User.builder().username(username).source(AuthSource.nomis).build();
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
        final var userOptional = user.isMaster() ? Optional.of(user) : userService.findUser(user.getUsername());

        // before we set, ensure user allowed to still change their password
        if (userOptional.map(u -> !u.isEnabled() || !u.isAccountNonLocked()).orElse(Boolean.TRUE)) {
            // failed, so let user know
            throw new LockedException("locked");
        }

        // if we're the master of this user record deal with the change of password here
        if (user.isMaster()) {
            changeAuthPassword(user, password);
        } else {
            alterUserService.changePassword(user.getUsername(), password);
        }
        user.removeToken(userToken);
        userRepository.save(user);
    }

}

package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;

import java.util.Map;

import static uk.gov.justice.digital.hmpps.oauth2server.maintain.CreateUserService.ALLOWED_ADDITIONAL_ROLES;

@Service
@Slf4j
public class AuthUserRoleService {
    private final UserEmailRepository userEmailRepository;
    private final TelemetryClient telemetryClient;

    public AuthUserRoleService(final UserEmailRepository userEmailRepository,
                               final TelemetryClient telemetryClient) {
        this.userEmailRepository = userEmailRepository;
        this.telemetryClient = telemetryClient;
    }

    @Transactional
    public void addRole(final String username, final String role) throws AuthUserRoleException {
        validate(role);

        // already checked that user exists
        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username).orElseThrow();

        log.info("Adding role {} to user {}", role, username);
        userEmail.getAuthorities().add(new Authority(role));
        telemetryClient.trackEvent("AuthUserRoleAddSuccess", Map.of("username", username, "role", role), null);
        userEmailRepository.save(userEmail);
    }

    @Transactional
    public void removeRole(final String username, final String role) {
        // already checked that user exists
        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username).orElseThrow();

        log.info("Removing role {} from user {}", role, username);
        userEmail.getAuthorities().removeIf(a -> a.getAuthority().equals(role));
        telemetryClient.trackEvent("AuthUserRoleRemoveSuccess", Map.of("username", username, "role", role), null);
        userEmailRepository.save(userEmail);
    }

    private void validate(final String role) throws AuthUserRoleException {
        if (StringUtils.isBlank(role)) {
            throw new AuthUserRoleException("role", "blank");
        }
        if (!ALLOWED_ADDITIONAL_ROLES.contains(role)) {
            throw new AuthUserRoleException("role", "invalid");
        }
    }

    @Getter
    public static class AuthUserRoleException extends Exception {
        private final String errorCode;
        private final String field;

        public AuthUserRoleException(final String field, final String errorCode) {
            super(String.format("Add role failed for field %s with reason: %s", field, errorCode));

            this.field = field;
            this.errorCode = errorCode;
        }
    }
}

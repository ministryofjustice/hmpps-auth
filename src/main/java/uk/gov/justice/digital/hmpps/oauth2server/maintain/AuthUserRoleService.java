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
import java.util.Set;


@Service
@Slf4j
public class AuthUserRoleService {
    static final Map<String, String> ALLOWED_AUTH_USER_ROLES = Map.of(
            "ROLE_LICENCE_VARY", "Licence Variation",
            "ROLE_LICENCE_RO", "Licence Responsible Officer",
            "ROLE_GLOBAL_SEARCH", "Global Search",
            "ROLE_PECS_POLICE", "PECS Police",
            "ROLE_PECS_SUPPLIER", "PECS Supplier");

    private final UserEmailRepository userEmailRepository;
    private final TelemetryClient telemetryClient;

    public AuthUserRoleService(final UserEmailRepository userEmailRepository,
                               final TelemetryClient telemetryClient) {
        this.userEmailRepository = userEmailRepository;
        this.telemetryClient = telemetryClient;
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void addRole(final String username, final String role, final String modifier) throws AuthUserRoleException {
        final var roleFormatted = formatRole(role);

        // already checked that user exists
        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username).orElseThrow();

        validate(roleFormatted, userEmail.getAuthorities());

        log.info("Adding role {} to user {}", roleFormatted, username);
        userEmail.getAuthorities().add(new Authority(roleFormatted));
        telemetryClient.trackEvent("AuthUserRoleAddSuccess", Map.of("username", username, "role", roleFormatted, "admin", modifier), null);
        userEmailRepository.save(userEmail);
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void removeRole(final String username, final String role, final String modifier) throws AuthUserRoleException {
        // already checked that user exists
        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username).orElseThrow();

        final var roleFormatted = formatRole(role);
        if (userEmail.getAuthorities().stream().map(Authority::getAuthority).noneMatch(a -> a.equals(roleFormatted))) {
            throw new AuthUserRoleException("role", "missing");
        }

        log.info("Removing role {} from user {}", roleFormatted, username);
        userEmail.getAuthorities().removeIf(a -> a.getAuthority().equals(roleFormatted));
        telemetryClient.trackEvent("AuthUserRoleRemoveSuccess", Map.of("username", username, "role", roleFormatted, "admin", modifier), null);
        userEmailRepository.save(userEmail);
    }

    private void validate(final String role, final Set<Authority> authorities) throws AuthUserRoleException {
        if (role.length() <= Authority.ROLE_PREFIX.length()) {
            throw new AuthUserRoleException("role", "blank");
        }
        if (!ALLOWED_AUTH_USER_ROLES.containsKey(role)) {
            throw new AuthUserRoleException("role", "invalid");
        }
        if (authorities.stream().map(Authority::getAuthority).anyMatch(a -> a.equals(role))) {
            throw new AuthUserRoleExistsException();
        }
    }

    private String formatRole(final String role) {
        return Authority.addRolePrefixIfNecessary(StringUtils.upperCase(StringUtils.trim(role)));
    }

    public Map<String, String> getAllRoles() {
        return ALLOWED_AUTH_USER_ROLES;
    }

    public static class AuthUserRoleExistsException extends AuthUserRoleException {
        public AuthUserRoleExistsException() {
            super("role", "exists");
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

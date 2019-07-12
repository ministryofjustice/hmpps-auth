package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.RoleRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
@AllArgsConstructor
public class AuthUserRoleService {

    private final UserEmailRepository userEmailRepository;
    private final RoleRepository roleRepository;
    private final TelemetryClient telemetryClient;

    @Transactional(transactionManager = "authTransactionManager")
    public void addRole(final String username, final String roleCode, final String modifier, final Collection<? extends GrantedAuthority> authorities) throws AuthUserRoleException {
        final var roleFormatted = formatRole(roleCode);

        // already checked that user exists
        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username).orElseThrow();

        // check that role exists
        final var role = roleRepository.findByAuthority(roleFormatted).orElseThrow(() -> new AuthUserRoleException("role", "notfound"));

        if (userEmail.getAuthorities().contains(role)) {
            throw new AuthUserRoleExistsException();
        }

        if (!getAssignableRoles(modifier, authorities).contains(role)) {
            throw new AuthUserRoleException("role", "invalid");
        }

        log.info("Adding role {} to user {}", roleFormatted, username);
        userEmail.getAuthorities().add(role);
        telemetryClient.trackEvent("AuthUserRoleAddSuccess", Map.of("username", username, "role", roleFormatted, "admin", modifier), null);
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void removeRole(final String username, final String roleCode, final String modifier, final Collection<? extends GrantedAuthority> authorities) throws AuthUserRoleException {
        // already checked that user exists
        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username).orElseThrow();

        final var roleFormatted = formatRole(roleCode);
        final var role = roleRepository.findByAuthority(roleFormatted).orElseThrow(() -> new AuthUserRoleException("role", "notfound"));

        if (!userEmail.getAuthorities().contains(role)) {
            throw new AuthUserRoleException("role", "missing");
        }

        if (!getAssignableRoles(modifier, authorities).contains(role)) {
            throw new AuthUserRoleException("role", "invalid");
        }

        log.info("Removing role {} from user {}", roleFormatted, username);
        userEmail.getAuthorities().removeIf(role::equals);
        telemetryClient.trackEvent("AuthUserRoleRemoveSuccess", Map.of("username", username, "role", roleFormatted, "admin", modifier), null);
    }

    private String formatRole(final String role) {
        return Authority.addRolePrefixIfNecessary(StringUtils.upperCase(StringUtils.trim(role)));
    }

    public List<Authority> getAllRoles() {
        return roleRepository.findAllByOrderByRoleName();
    }

    public Set<Authority> getAssignableRoles(final String username, final Collection<? extends GrantedAuthority> authorities) {
        if (canMaintainAuthUsers(authorities)) {
            // only allow oauth admins to see that role
            return getAllRoles().stream().filter(r -> !"ROLE_OAUTH_ADMIN".equals(r.getAuthorityName()) || canAddAuthClients(authorities)).collect(Collectors.toSet());
        }
        // TODO: return roles for all groups
        return Set.copyOf(getAllRoles());
    }

    public static class AuthUserRoleExistsException extends AuthUserRoleException {
        public AuthUserRoleExistsException() {
            super("role", "exists");
        }
    }

    public static boolean canMaintainAuthUsers(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_MAINTAIN_OAUTH_USERS"::equals);
    }

    public static boolean canAddAuthClients(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_OAUTH_ADMIN"::equals);
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

package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service("userDetailsService")
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final UserService userService;
    private final String apiCaseloadId;
    private final boolean oracleForNomis;

    public UserDetailsServiceImpl(final UserService userService,
                                  @Value("${application.caseload.id}") final String apiCaseloadId,
                                  @Value("#{environment.acceptsProfiles('oracle')}") final boolean oracleForNomis) {
        this.apiCaseloadId = apiCaseloadId;
        this.userService = userService;
        this.oracleForNomis = oracleForNomis;
    }

    @Override
    @Cacheable("loadUserByUsername")
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        final var staffUserAccount = userService.getUserByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
        if (staffUserAccount.filterByCaseload(apiCaseloadId).isEmpty()) {
            throw new UnapprovedClientAuthenticationException(format("User does not have access to caseload %s", apiCaseloadId));
        }

        final Set<GrantedAuthority> authorities = staffUserAccount.filterRolesByCaseload(apiCaseloadId).stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + StringUtils.upperCase(RegExUtils.replaceAll(role.getRole().getCode(), "-", "_"))))
                .collect(Collectors.toSet());

        final var accountDetail = staffUserAccount.getAccountDetail();
        final boolean enabled;
        final boolean credentialsNonExpired;
        final boolean accountNonLocked;

        final var status = accountDetail.getStatus();
        switch (status) {

            case OPEN:
            case EXPIRED_GRACE:
                enabled = true;
                credentialsNonExpired = true;
                accountNonLocked = true;
                break;
            case EXPIRED:
                enabled = true;
                credentialsNonExpired = false;
                accountNonLocked = true;
                break;
            case LOCKED:
            case LOCKED_TIMED:
            case EXPIRED_LOCKED_TIMED:
            case EXPIRED_GRACE_LOCKED_TIMED:
            case EXPIRED_LOCKED:
            case EXPIRED_GRACE_LOCKED:
                enabled = false;
                credentialsNonExpired = true;
                accountNonLocked = false;
                break;
            default:
                enabled = false;
                credentialsNonExpired = true;
                accountNonLocked = true;
        }

        // when running using oracle for nomis datasource then passwords need prefixing when retrieved
        final var password = (oracleForNomis ? "{oracle}" : "") + staffUserAccount.getPassword();

        return new UserDetailsImpl(staffUserAccount.getUsername(), staffUserAccount.getStaff().getName(), password,
                enabled, true, credentialsNonExpired, accountNonLocked, authorities);
    }

    @Override
    public UserDetails loadUserDetails(final PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName());
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.security;

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
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service("userDetailsService")
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final UserService userService;
    private final String apiCaseloadId;

    public UserDetailsServiceImpl(UserService userService,
                                  @Value("${application.caseload.id}") String apiCaseloadId) {
        this.apiCaseloadId = apiCaseloadId;
        this.userService = userService;
    }

    @Override
    @Cacheable("loadUserByUsername")
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {

        StaffUserAccount userByUsername = userService.getUserByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
        if (userByUsername.filterByCaseload(apiCaseloadId).isEmpty()) {
            throw new UnapprovedClientAuthenticationException(format("User does not have access to caseload %s", apiCaseloadId));
        }

        Set<GrantedAuthority> authorities = userByUsername.filterRolesByCaseload(apiCaseloadId).stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + StringUtils.upperCase(StringUtils.replaceAll(role.getRole().getCode(), "-", "_"))))
                .collect(Collectors.toSet());

        UserDetailsImpl userDetails = new UserDetailsImpl(username, authorities);

        AccountDetail accountDetail = userByUsername.getAccountDetail();
        userDetails.setAccountNonExpired(true);
        userDetails.setAccountNonLocked(true);
        userDetails.setEnabled(false);
        userDetails.setCredentialsNonExpired(true);

        AccountStatus status = accountDetail.getStatus();
        switch (status) {

            case OPEN:
                userDetails.setEnabled(true);
                break;
            case EXPIRED_GRACE:
                userDetails.setEnabled(true);
                break;
            case LOCKED_TIMED:
                userDetails.setAccountNonLocked(false);
                break;
            case LOCKED:
                userDetails.setAccountNonLocked(false);
                break;
            case EXPIRED:
                userDetails.setEnabled(true);
                userDetails.setAccountNonExpired(false);
                break;
            case EXPIRED_LOCKED_TIMED:
                userDetails.setAccountNonLocked(false);
                break;
            case EXPIRED_GRACE_LOCKED_TIMED:
                userDetails.setAccountNonLocked(false);
                break;
            case EXPIRED_LOCKED:
                userDetails.setAccountNonLocked(false);
                break;
            case EXPIRED_GRACE_LOCKED:
                userDetails.setAccountNonLocked(false);
                break;
        }

        return userDetails;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName());
    }
}

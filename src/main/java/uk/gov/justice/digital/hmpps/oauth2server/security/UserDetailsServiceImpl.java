package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.model.UserCaseloadRole;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service("userDetailsService")
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
	private final UserService userService;

	public UserDetailsServiceImpl(UserService userService) {
		this.userService = userService;
	}

	@Override
	@Cacheable("loadUserByUsername")
	public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {

		userService.getUserByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
		List<UserCaseloadRole> roles = userService.getRolesByUsername(username, false);

		Set<GrantedAuthority> authorities = roles.stream()
				.filter(Objects::nonNull)
				.map(role -> new SimpleGrantedAuthority("ROLE_" + StringUtils.upperCase(StringUtils.replaceAll(role.getRole().getCode(),"-", "_"))))
				.collect(Collectors.toSet());

		return new UserDetailsImpl(username, null, authorities);
	}

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName());
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userDetailsService")
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final UserService userService;

    public UserDetailsServiceImpl(final UserService userService) {
        this.userService = userService;
    }

    @Override
    @Cacheable("loadUserByUsername")
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return userService.findUser(username).orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Override
    public UserDetails loadUserDetails(final PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName());
    }
}

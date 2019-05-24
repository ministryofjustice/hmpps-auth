package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.oasys.model.OasysUser;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service("oasysUserDetailsService")
@Transactional(readOnly = true)
public class OasysUserDetailsServiceImpl implements UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private final OasysUserService userService;

    @PersistenceContext(unitName = "oasys")
    private EntityManager oasysEntityManager;

    @PersistenceContext(unitName = "auth")
    private EntityManager authEntityManager;

    @Autowired
    public OasysUserDetailsServiceImpl(final OasysUserService userService) {
        this.userService = userService;
    }

    @Override
    @Cacheable("loadUserByUsername")
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        final var userPersonDetails = userService.findUser(username).orElseThrow(() -> new UsernameNotFoundException(username));

        // ensure that any changes to user details past this point are not persisted - e.g. by calling
        // CredentialsContainer.eraseCredentials
        if (userPersonDetails instanceof OasysUser) {
            oasysEntityManager.detach(userPersonDetails);
        } else {
            authEntityManager.detach(userPersonDetails);
        }
        return userPersonDetails;
    }

    @Override
    public UserDetails loadUserDetails(final PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return loadUserByUsername(token.getName());
    }
}

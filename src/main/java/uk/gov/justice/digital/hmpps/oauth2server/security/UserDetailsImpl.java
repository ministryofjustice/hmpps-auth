package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
@Data
public class UserDetailsImpl implements UserDetails {
    private final String username;
    private final String name;
    private boolean enabled;
    private boolean credentialsNonExpired;
    private boolean accountNonLocked;
    private boolean accountNonExpired;
    private String password;

    private final Set<GrantedAuthority> authorities = new HashSet<>();

    public UserDetailsImpl(final String username, final String name,
                           final Collection<GrantedAuthority> authorities) {
        this.username = username;
        this.name = name;
        this.authorities.addAll(authorities);
    }
}

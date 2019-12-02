package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@SuppressWarnings("serial")
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDetailsImpl extends User implements UserPersonDetails {
    // This class is serialized to the database (oauth_code table) during /auth/oauth/authorize and then read back
    // during /auth/oauth/token.  Therefore implemented serial version UID, although breaking changes should increment.
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String firstName;
    private final String authSource;
    private final String userId;

    UserDetailsImpl(final String username, final String name, final String password,
                    final boolean enabled, final boolean accountNonExpired, final boolean credentialsNonExpired,
                    final boolean accountNonLocked, final Collection<? extends GrantedAuthority> authorities, final String authSource, final String userId) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.name = name;
        this.firstName = name;
        this.authSource = authSource;
        this.userId = userId;
    }

    public UserDetailsImpl(final String username, final String name, final Collection<GrantedAuthority> authorities, final String authSource, final String userId) {
        super(username, "", authorities);
        this.name = name;
        this.firstName = name;
        this.authSource = authSource;
        this.userId = userId;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public uk.gov.justice.digital.hmpps.oauth2server.auth.model.User toUser() {
        throw new IllegalStateException("Can't be converted into user");
    }
}

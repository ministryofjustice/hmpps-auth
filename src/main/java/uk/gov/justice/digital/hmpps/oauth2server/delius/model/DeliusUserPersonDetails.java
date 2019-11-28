package uk.gov.justice.digital.hmpps.oauth2server.delius.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import java.util.Collection;

@Builder
@Data
public class DeliusUserPersonDetails implements UserPersonDetails {
    private String surname;
    private String firstName;
    private String username;
    private boolean locked;
    private Collection<? extends GrantedAuthority> roles;

    @Override
    public String getUserId() {
        return username;
    }

    @Override
    public String getName() {
        return String.format("%s %s", firstName, surname);
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public String getAuthSource() {
        return "delius";
    }

    @Override
    public void eraseCredentials() {
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getPassword() {
        return "password";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

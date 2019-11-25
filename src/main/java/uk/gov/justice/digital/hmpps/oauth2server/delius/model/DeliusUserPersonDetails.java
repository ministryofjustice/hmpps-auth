package uk.gov.justice.digital.hmpps.oauth2server.delius.model;

import org.springframework.security.core.GrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import java.util.Collection;

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
    public String getFirstName() {
        return firstName;
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
    public String getUsername() {
        return username;
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

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRoles(Collection<? extends GrantedAuthority> roles) {
        this.roles = roles;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}

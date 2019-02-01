package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.CredentialsContainer;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;

@Entity
@Table(name = "USER_EMAIL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"username"})
public class UserEmail implements UserPersonDetails, CredentialsContainer {

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /**
     * Whether we are masters of this user record here in auth
     */
    @Column(name = "master", nullable = false)
    private boolean master;

    @Column(name = "password_expiry")
    private LocalDateTime passwordExpiry = LocalDateTime.now();

    @OneToOne(cascade = ALL)
    @JoinColumn(name = "username")
    @PrimaryKeyJoinColumn
    private Person person;

    @OneToMany(cascade = ALL, orphanRemoval = true, fetch = EAGER)
    @JoinColumn(name = "username", nullable = false)
    private Set<Authority> authorities = new HashSet<>();

    public UserEmail(final String username) {
        this.username = username;
    }

    public UserEmail(final String username, final String email, final boolean verified, final boolean locked) {
        this.username = username;
        this.email = email;
        this.verified = verified;
        this.locked = locked;
    }

    @Override
    public void eraseCredentials() {
        password = null;
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
        return passwordExpiry.isAfter(LocalDateTime.now());
    }

    @Override
    public String getName() {
        return person != null ? person.getName() : username;
    }

    @Override
    public String getFirstName() {
        return person != null ? person.getFirstName() : username;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
}

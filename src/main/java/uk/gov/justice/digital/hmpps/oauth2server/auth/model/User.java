package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.security.core.CredentialsContainer;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static javax.persistence.FetchType.EAGER;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"id"})
public class User implements UserPersonDetails, CredentialsContainer {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "user_id", updatable = false, nullable = false)
    @Type(type = "uuid-char")
    private UUID id;

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
    @Column(name = "source", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthSource source;

    /**
     * Used for NOMIS accounts to force change password so that they don't get locked out due to not changing password
     */
    @Column(name = "password_expiry")
    private LocalDateTime passwordExpiry = LocalDateTime.now();

    @Column(name = "last_logged_in")
    private LocalDateTime lastLoggedIn = LocalDateTime.now();

    @Embedded
    private Person person;

    @OneToMany(fetch = EAGER)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Authority> authorities = new HashSet<>();

    @OneToMany
    @JoinTable(name = "user_group",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Set<Group> groups = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private Set<UserToken> tokens = new HashSet<>();

    public static User of(final String username) {
        return User.builder().username(username).build();
    }

    public static UserBuilder builder() {
        return new UserBuilder();
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

    @Override
    public String getAuthSource() {
        return source.toString();
    }

    @Override
    public User toUser() {
        return this;
    }

    @Override
    public String getUserId() {
        return id.toString();
    }

    public UserToken createToken(final TokenType tokenType) {
        final var token = new UserToken(tokenType, this);
        // equals and hashcode by token type so remove will remove any token of same type
        tokens.remove(token);
        tokens.add(token);
        return token;
    }

    public void removeToken(final UserToken userToken) {
        tokens.remove(userToken);
    }

    public boolean isMaster() {
        return source == AuthSource.auth;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", verified=" + verified +
                ", locked=" + locked +
                ", enabled=" + enabled +
                ", source=" + source +
                ", passwordExpiry=" + passwordExpiry +
                ", lastLoggedIn=" + lastLoggedIn +
                ", person=" + person +
                ", authorities=" + authorities +
                '}';
    }

    public static class UserBuilder {
        private UUID id;
        private String username;
        private String password;
        private String email;
        private boolean verified;
        private boolean locked;
        private boolean enabled;
        private AuthSource source;
        private LocalDateTime passwordExpiry = LocalDateTime.now();
        private LocalDateTime lastLoggedIn = LocalDateTime.now();
        private Person person;
        private Set<Authority> authorities = new HashSet<>();
        private Set<Group> groups = new HashSet<>();
        private Set<UserToken> tokens = new HashSet<>();

        UserBuilder() {
        }

        public UserBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        public UserBuilder username(final String username) {
            this.username = username;
            return this;
        }

        public UserBuilder password(final String password) {
            this.password = password;
            return this;
        }

        public UserBuilder email(final String email) {
            this.email = email;
            return this;
        }

        public UserBuilder verified(final boolean verified) {
            this.verified = verified;
            return this;
        }

        public UserBuilder locked(final boolean locked) {
            this.locked = locked;
            return this;
        }

        public UserBuilder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserBuilder source(final AuthSource source) {
            this.source = source;
            return this;
        }

        public UserBuilder passwordExpiry(final LocalDateTime passwordExpiry) {
            this.passwordExpiry = passwordExpiry;
            return this;
        }

        public UserBuilder lastLoggedIn(final LocalDateTime lastLoggedIn) {
            this.lastLoggedIn = lastLoggedIn;
            return this;
        }

        public UserBuilder person(final Person person) {
            this.person = person;
            return this;
        }

        public UserBuilder authorities(final Set<Authority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public UserBuilder groups(final Set<Group> groups) {
            this.groups = groups;
            return this;
        }

        public UserBuilder tokens(final Set<UserToken> tokens) {
            this.tokens = tokens;
            return this;
        }

        public User build() {
            return new User(id, username, password, email, verified, locked, enabled, source, passwordExpiry, lastLoggedIn, person, authorities, groups, tokens);
        }

        public String toString() {
            return "User.UserBuilder(id=" + this.id + ", username=" + this.username + ", password=" + this.password + ", email=" + this.email + ", verified=" + this.verified + ", locked=" + this.locked + ", enabled=" + this.enabled + ", source=" + this.source + ", passwordExpiry=" + this.passwordExpiry + ", lastLoggedIn=" + this.lastLoggedIn + ", person=" + this.person + ", authorities=" + this.authorities + ", groups=" + this.groups + ", tokens=" + this.tokens + ")";
        }
    }
}

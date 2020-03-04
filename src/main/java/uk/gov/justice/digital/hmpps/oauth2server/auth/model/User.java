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

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "mobile_verified", nullable = false)
    private boolean mobileVerified;

    @Column(name = "mfa_preference")
    @Enumerated(EnumType.STRING)
    private MfaPreferenceType mfaPreference = MfaPreferenceType.EMAIL;

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
                ", mobile=" + mobile +
                ", mobileVerified=" + mobileVerified +
                ", person=" + person +
                ", authorities=" + authorities +
                '}';
    }

    public UUID getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getEmail() {
        return this.email;
    }

    public boolean isVerified() {
        return this.verified;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public AuthSource getSource() {
        return this.source;
    }

    public LocalDateTime getPasswordExpiry() {
        return this.passwordExpiry;
    }

    public LocalDateTime getLastLoggedIn() {
        return this.lastLoggedIn;
    }

    public String getMobile() {
        return mobile;
    }

    public boolean isMobileVerified() {
        return mobileVerified;
    }

    public MfaPreferenceType getMfaPreference() {
        return mfaPreference;
    }

    public Person getPerson() {
        return this.person;
    }

    public Set<Authority> getAuthorities() {
        return this.authorities;
    }

    public Set<Group> getGroups() {
        return this.groups;
    }

    public Set<UserToken> getTokens() {
        return this.tokens;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSource(AuthSource source) {
        this.source = source;
    }

    public void setPasswordExpiry(LocalDateTime passwordExpiry) {
        this.passwordExpiry = passwordExpiry;
    }

    public void setLastLoggedIn(LocalDateTime lastLoggedIn) {
        this.lastLoggedIn = lastLoggedIn;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public void setMobileVerified(final boolean mobileVerified) {
        this.mobileVerified = mobileVerified;
    }


    public void setMfaPreference(final MfaPreferenceType mfaPreference) {
        this.mfaPreference = mfaPreference;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    @AllArgsConstructor
    @Getter
    public enum MfaPreferenceType {
        EMAIL("email"),
        TEXT("text");

        private final String description;


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
        private String mobile;
        private boolean mobileVerified;
        private MfaPreferenceType mfaPreference = MfaPreferenceType.EMAIL;
        private Person person;
        private Set<Authority> authorities = new HashSet<>();
        private Set<Group> groups = new HashSet<>();
        private Set<UserToken> tokens = new HashSet<>();

        UserBuilder() {
        }

        public UserBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder verified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public UserBuilder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public UserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserBuilder source(AuthSource source) {
            this.source = source;
            return this;
        }

        public UserBuilder passwordExpiry(LocalDateTime passwordExpiry) {
            this.passwordExpiry = passwordExpiry;
            return this;
        }

        public UserBuilder lastLoggedIn(LocalDateTime lastLoggedIn) {
            this.lastLoggedIn = lastLoggedIn;
            return this;
        }

        public UserBuilder mobile(String mobile) {
            this.mobile = mobile;
            return this;
        }

        public UserBuilder mobileVerified(boolean mobileVerified) {
            this.mobileVerified = mobileVerified;
            return this;
        }

        public UserBuilder mfaPreference(MfaPreferenceType mfaPreference) {
            this.mfaPreference = mfaPreference;
            return this;
        }

        public UserBuilder person(Person person) {
            this.person = person;
            return this;
        }

        public UserBuilder authorities(Set<Authority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public UserBuilder groups(Set<Group> groups) {
            this.groups = groups;
            return this;
        }

        public UserBuilder tokens(Set<UserToken> tokens) {
            this.tokens = tokens;
            return this;
        }

        public User build() {
            return new User(id, username, password, email, verified, locked, enabled, source, passwordExpiry, lastLoggedIn, mobile, mobileVerified, mfaPreference, person, authorities, groups, tokens);
        }

        public String toString() {
            return "User.UserBuilder(id=" + this.id + ", username=" + this.username + ", password=" + this.password + ", email=" + this.email + ", verified=" + this.verified + ", locked=" + this.locked + ", enabled=" + this.enabled + ", source=" + this.source + ", passwordExpiry=" + this.passwordExpiry + ", lastLoggedIn=" + this.lastLoggedIn + ", mobile=" + this.mobile + ", mobileVerified=" + this.mobileVerified + ", mfaPreference=" + this.mfaPreference + ", person=" + this.person + ", authorities=" + this.authorities + ", groups=" + this.groups + ", tokens=" + this.tokens + ")";
        }
    }
}

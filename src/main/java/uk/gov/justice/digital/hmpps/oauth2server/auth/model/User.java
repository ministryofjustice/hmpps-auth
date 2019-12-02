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
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"id"})
@Builder
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
    @Builder.Default
    private LocalDateTime passwordExpiry = LocalDateTime.now();

    @Column(name = "last_logged_in")
    @Builder.Default
    private LocalDateTime lastLoggedIn = LocalDateTime.now();

    @Embedded
    private Person person;

    @OneToMany(fetch = EAGER)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Authority> authorities = new HashSet<>();

    @OneToMany
    @JoinTable(name = "user_group",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    @Builder.Default
    private Set<Group> groups = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private Set<UserToken> tokens = new HashSet<>();

    public static User of(final String username) {
        return User.builder().username(username).build();
    }

    public static User fromUserPersonDetails(final UserPersonDetails userPersonDetails) {
        return User.builder()
                .username(userPersonDetails.getUsername())
                .source(AuthSource.valueOf(userPersonDetails.getAuthSource()))
                .build();
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
}

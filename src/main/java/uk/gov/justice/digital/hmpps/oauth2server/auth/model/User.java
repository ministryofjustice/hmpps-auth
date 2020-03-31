package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.CredentialsContainer;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

    @Column(name = "mfa_preference")
    @Enumerated(EnumType.STRING)
    private MfaPreferenceType mfaPreference = MfaPreferenceType.EMAIL;

    @Embedded
    private Person person;

    @ElementCollection
    @CollectionTable(name = "USER_CONTACT", joinColumns = @JoinColumn(name = "user_id"))
    private Set<Contact> contacts = new HashSet<>();

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

    public String getMaskedMobile() {
        return "*******" + getMobile().substring(7);
    }

    public String getMaskedEmail() {
        final var emailCharacters = StringUtils.substringBefore(email, "@").length();
        final var emailCharactersReduced = Math.min(emailCharacters / 2, 6);
        return email.substring(0, emailCharactersReduced) + "******@******" + email.substring(email.length() - 7);
    }

    public String getMaskedSecondaryEmail() {
        final var emailCharacters = StringUtils.substringBefore(getSecondaryEmail(), "@").length();
        final var emailCharactersReduced = Math.min(emailCharacters / 2, 6);
        return getSecondaryEmail().substring(0, emailCharactersReduced) + "******@******" + getSecondaryEmail().substring(getSecondaryEmail().length() - 7);
    }

    public boolean mfaPreferenceVerified() {
        return mfaPreferenceTextVerified() || mfaPreferenceEmailVerified() || mfaPreferenceSecondaryEmailVerified();
    }

    public boolean mfaPreferenceTextVerified() {
        return mfaPreference == MfaPreferenceType.TEXT && isMobileVerified();
    }

    public boolean mfaPreferenceEmailVerified() {
        return mfaPreference == MfaPreferenceType.EMAIL && verified;
    }

    public boolean mfaPreferenceSecondaryEmailVerified() {
        return mfaPreference == MfaPreferenceType.SECONDARY_EMAIL && isSecondaryEmailVerified();
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
        return getContactValue(ContactType.MOBILE_PHONE);
    }

    public boolean isMobileVerified() {
        return isContactVerified(ContactType.MOBILE_PHONE);
    }

    public Contact addContact(final ContactType type, final String value) {
        final var contact = new Contact(type, value, false);
        // equals and hashcode by contact type so remove will remove any contact of same type
        contacts.remove(contact);
        contacts.add(contact);
        return contact;
    }

    private boolean isContactVerified(final ContactType mobilePhone) {
        return findContact(mobilePhone).map(Contact::getVerified).orElse(false);
    }

    private String getContactValue(final ContactType mobilePhone) {
        return findContact(mobilePhone).map(Contact::getValue).orElse(null);
    }

    public String getSecondaryEmail() {
        return getContactValue(ContactType.SECONDARY_EMAIL);
    }

    public boolean isSecondaryEmailVerified() {
        return isContactVerified(ContactType.SECONDARY_EMAIL);
    }

    public Optional<Contact> findContact(final ContactType type) {
        return contacts.stream()
                .filter(c -> c.getType() == type)
                .findFirst();
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

    public void setId(final UUID id) {
        this.id = id;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setVerified(final boolean verified) {
        this.verified = verified;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setSource(final AuthSource source) {
        this.source = source;
    }

    public void setPasswordExpiry(final LocalDateTime passwordExpiry) {
        this.passwordExpiry = passwordExpiry;
    }

    public void setLastLoggedIn(final LocalDateTime lastLoggedIn) {
        this.lastLoggedIn = lastLoggedIn;
    }

    public void setMfaPreference(final MfaPreferenceType mfaPreference) {
        this.mfaPreference = mfaPreference;
    }

    public void setPerson(final Person person) {
        this.person = person;
    }

    public void setAuthorities(final Set<Authority> authorities) {
        this.authorities = authorities;
    }

    public void setGroups(final Set<Group> groups) {
        this.groups = groups;
    }

    public boolean hasVerifiedMfaMethod() {
        return !getAllowedMfaPreferences().isEmpty();
    }

    public Optional<MfaPreferenceType> calculateMfaFromPreference() {
        final var preferences = getAllowedMfaPreferences();
        return preferences.contains(mfaPreference) ? Optional.of(mfaPreference) : preferences.stream().findFirst();
    }

    @NotNull
    private List<MfaPreferenceType> getAllowedMfaPreferences() {
        final var preferences = new ArrayList<MfaPreferenceType>();
        if (StringUtils.isNotEmpty(email) && verified) preferences.add(MfaPreferenceType.EMAIL);
        findContact(ContactType.MOBILE_PHONE)
                .filter(c -> StringUtils.isNotBlank(c.getValue()) && c.getVerified())
                .ifPresent(c -> preferences.add(MfaPreferenceType.TEXT));
        findContact(ContactType.SECONDARY_EMAIL)
                .filter(c -> StringUtils.isNotBlank(c.getValue()) && c.getVerified())
                .ifPresent(c -> preferences.add(MfaPreferenceType.SECONDARY_EMAIL));
        return preferences;
    }

    public Set<Contact> getContacts() {
        return contacts;
    }

    @AllArgsConstructor
    @Getter
    public enum MfaPreferenceType {
        EMAIL("email"),
        TEXT("text"),
        SECONDARY_EMAIL("secondary email");

        private final String description;
    }

    @AllArgsConstructor
    @Getter
    public enum EmailType {
        PRIMARY("primary"),
        SECONDARY("secondary");

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
        private Set<Contact> contacts = new HashSet<>();
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

        public UserBuilder mobile(final String mobile) {
            this.mobile = mobile;
            return this;
        }

        public UserBuilder mobileVerified(final boolean mobileVerified) {
            this.mobileVerified = mobileVerified;
            return this;
        }

        public UserBuilder mfaPreference(final MfaPreferenceType mfaPreference) {
            this.mfaPreference = mfaPreference;
            return this;
        }

        public UserBuilder person(final Person person) {
            this.person = person;
            return this;
        }

        public UserBuilder contacts(final Set<Contact> contacts) {
            this.contacts = contacts;
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
            final var user = new User(id, username, password, email, verified, locked, enabled, source, passwordExpiry, lastLoggedIn, mfaPreference, person, contacts, authorities, groups, tokens);
            if (mobile != null || mobileVerified) {
                final var contact = user.addContact(ContactType.MOBILE_PHONE, mobile);
                contact.setVerified(mobileVerified);
            }
            return user;
        }

        public String toString() {
            return "User.UserBuilder(id=" + this.id + ", username=" + this.username + ", password=" + this.password + ", email=" + this.email + ", verified=" + this.verified + ", locked=" + this.locked + ", enabled=" + this.enabled + ", source=" + this.source + ", passwordExpiry=" + this.passwordExpiry + ", lastLoggedIn=" + this.lastLoggedIn + ", mfaPreference=" + this.mfaPreference + ", person=" + this.person + ", contacts=" + this.contacts + ", authorities=" + this.authorities + ", groups=" + this.groups + ", tokens=" + this.tokens + ")";
        }
    }
}

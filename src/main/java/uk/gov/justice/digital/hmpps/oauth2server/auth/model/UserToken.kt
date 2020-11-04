package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "USER_TOKEN")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"user", "tokenType"})
@ToString(exclude = "user")
public class UserToken {

    @Id
    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "token_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @Column(name = "token_expiry", nullable = false)
    private LocalDateTime tokenExpiry;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    UserToken(final TokenType tokenType, final User user) {
        this.token = tokenType == TokenType.MFA_CODE || tokenType == TokenType.MOBILE ? generateIntAsString() : UUID.randomUUID().toString();
        this.tokenType = tokenType;
        this.user = user;

        resetExpiry();
    }

    void resetExpiry() {
        final var now = LocalDateTime.now();
        this.tokenExpiry = tokenType == TokenType.CHANGE || tokenType == TokenType.MFA ? now.plusMinutes(20) : now.plusDays(1);
    }

    private String generateIntAsString() {
        try {
            final var random = SecureRandom.getInstance("DRBG").nextInt(1000000);
            return String.format("%06d", random);

        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasTokenExpired() {
        return tokenExpiry.isBefore(LocalDateTime.now());
    }

    public String getToken() {
        return this.token;
    }

    public TokenType getTokenType() {
        return this.tokenType;
    }

    public LocalDateTime getTokenExpiry() {
        return this.tokenExpiry;
    }

    public User getUser() {
        return this.user;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public void setTokenType(final TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public void setTokenExpiry(final LocalDateTime tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    @AllArgsConstructor
    public enum TokenType {
        RESET("ResetPassword"),
        VERIFIED("VerifiedPassword"),
        CHANGE("ChangePassword"),
        MFA("MFA"),
        MFA_CODE("MFACode"),
        SECONDARY("SecondEmailVerifyCode"),
        MOBILE("MobileVerifyCode");

        private final String description;

        public String getDescription() {
            return this.description;
        }
    }
}

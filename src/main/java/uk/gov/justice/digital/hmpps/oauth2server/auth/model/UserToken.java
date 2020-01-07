package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "USER_TOKEN")
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"tokenType"})
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
        this.token = UUID.randomUUID().toString();
        this.tokenType = tokenType;
        this.user = user;

        final var now = LocalDateTime.now();
        this.tokenExpiry = tokenType == TokenType.CHANGE || tokenType == TokenType.MFA ? now.plusMinutes(20) : now.plusDays(1);
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

    @AllArgsConstructor
    public enum TokenType {
        RESET("ResetPassword"),
        VERIFIED("VerifiedPassword"),
        CHANGE("ChangePassword"),
        MFA("MFA"),
        MFA_CODE("MFACode");

        private final String description;

        public String getDescription() {
            return this.description;
        }
    }
}

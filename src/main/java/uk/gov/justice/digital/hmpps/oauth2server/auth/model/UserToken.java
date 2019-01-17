package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "USER_TOKEN")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"token"})
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
    @JoinColumn(name = "username")
    private UserEmail userEmail;

    public UserToken(final TokenType tokenType, final UserEmail userEmail) {
        this.token = UUID.randomUUID().toString();
        this.tokenType = tokenType;
        this.userEmail = userEmail;

        final var now = LocalDateTime.now();
        this.tokenExpiry = tokenType == TokenType.CHANGE ? now.plusMinutes(30) : now.plusDays(1);
    }

    public boolean hasTokenExpired() {
        return tokenExpiry.isBefore(LocalDateTime.now());
    }

    public enum TokenType {
        RESET,
        VERIFIED,
        CHANGE
    }
}

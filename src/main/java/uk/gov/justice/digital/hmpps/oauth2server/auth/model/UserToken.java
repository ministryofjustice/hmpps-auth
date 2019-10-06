package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "USER_TOKEN")
@Data
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
        this.tokenExpiry = tokenType == TokenType.CHANGE ? now.plusMinutes(20) : now.plusDays(1);
    }

    public boolean hasTokenExpired() {
        return tokenExpiry.isBefore(LocalDateTime.now());
    }

    @Getter
    @AllArgsConstructor
    public enum TokenType {
        RESET("Reset"),
        VERIFIED("Verified"),
        CHANGE("Change");

        private final String description;
    }
}

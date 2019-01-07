package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_EMAIL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"username"})
public class UserEmail {

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "token")
    private String token;

    @Column(name = "token_type")
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    public UserEmail(final String username) {
        this.username = username;
    }

    public enum TokenType {
        RESET,
        VERIFIED
    }

    public void setToken(final TokenType tokenType, final String token) {
        this.tokenType = tokenType;
        this.token = token;
        this.tokenExpiry = LocalDateTime.now().plusDays(1);
    }

    public void clearToken() {
        this.tokenType = null;
        this.token = null;
        this.tokenExpiry = null;
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "USER_RETRIES")
@Data
@EqualsAndHashCode(of = {"username"})
public class UserRetries {

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "retry_count")
    private int retryCount;

    public UserRetries(final String username, final int retryCount) {
        this.username = username;
        this.retryCount = retryCount;
    }

    public UserRetries() {
    }

    public void incrementRetryCount() {
        retryCount++;
    }

    public String getUsername() {
        return this.username;
    }

    public int getRetryCount() {
        return this.retryCount;
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "USER_RETRIES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"username"})
public class UserRetries {

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "retry_count")
    private int retryCount;

    public void incrementRetryCount() {
        retryCount++;
    }
}

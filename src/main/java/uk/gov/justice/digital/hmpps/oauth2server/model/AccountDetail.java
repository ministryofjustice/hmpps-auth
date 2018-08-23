package uk.gov.justice.digital.hmpps.oauth2server.model;

import lombok.*;
import org.hibernate.annotations.Type;
import uk.gov.justice.digital.hmpps.oauth2server.security.AccountStatus;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "V_TAG_DBA_USERS")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"username"})
@ToString(of = { "username" })
public class AccountDetail {

    @Id
    @Column(name = "USERNAME", nullable = false)
    private String username;

    @Column(name = "ACCOUNT_STATUS", nullable = false)
    private String accountStatus;

    @Column(name = "LOCK_DATE")
    private LocalDateTime lockDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

    @Column(name = "CREATED", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "PROFILE")
    private String profile;

    @Type(type = "yes_no")
    @Column(name = "LOGGED_IN", nullable = false)
    private boolean loggedIn;

    @Type(type = "yes_no")
    @Column(name = "LOCKED_FLAG", nullable = false)
    private boolean locked;

    @Type(type = "yes_no")
    @Column(name = "EXPIRED_FLAG", nullable = false)
    private boolean expired;

    @Column(name = "USER_TYPE_DESCRIPTION")
    private String typeDescription;

    public AccountStatus getStatus() {
        return AccountStatus.get(accountStatus);
    }

}
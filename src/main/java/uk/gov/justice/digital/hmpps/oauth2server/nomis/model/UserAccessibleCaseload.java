package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "USER_ACCESSIBLE_CASELOADS")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"caseload", "user"})
@ToString(of = {"caseload", "user", "startDate"})
public class UserAccessibleCaseload implements Serializable {

    @EmbeddedId
    private UserCaseloadIdentity id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CASELOAD_ID", updatable = false, insertable = false)
    private Caseload caseload;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USERNAME", updatable = false, insertable = false)
    private StaffUserAccount user;

    @Column(name = "START_DATE")
    private LocalDate startDate;
}

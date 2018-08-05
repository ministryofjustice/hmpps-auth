package uk.gov.justice.digital.hmpps.oauth2server.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "USER_CASELOAD_ROLES")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"id"})
@ToString(of = "id")
public class UserCaseloadRole implements Serializable {

    @EmbeddedId
    private UserCaseloadRoleIdentity id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", updatable = false, insertable = false)
    private Role role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CASELOAD_ID", updatable = false, insertable = false)
    private Caseload caseload;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USERNAME", updatable = false, insertable = false)
    private StaffUserAccount user;
}

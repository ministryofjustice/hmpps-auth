package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"username", "roleId", "caseload"})
@Embeddable
@ToString(of = {"username", "roleId", "caseload"})
public class UserCaseloadRoleIdentity implements Serializable {

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "username")
    private String username;

    @Column(name = "caseload_id")
    private String caseload;

}

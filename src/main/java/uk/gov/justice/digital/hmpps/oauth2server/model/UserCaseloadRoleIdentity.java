package uk.gov.justice.digital.hmpps.oauth2server.model;

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
public class UserCaseloadRoleIdentity implements Serializable {

    @Column(name = "username")
    private String username;

    @Column(name = "role_id")
    private String roleId;

    @Column(name = "caseload_id")
    private String caseload;

}

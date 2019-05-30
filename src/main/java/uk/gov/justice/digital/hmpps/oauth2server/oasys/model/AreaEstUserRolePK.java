package uk.gov.justice.digital.hmpps.oauth2server.oasys.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaEstUserRolePK implements Serializable {
    @Column(name = "OASYS_USER_CODE")
    @Id
    private String oasysUserCode;
    @Column(name = "REF_ROLE_CODE")
    @Id
    private String refRoleCode;
    @Column(name = "CT_AREA_EST_CODE")
    @Id
    private String ctAreaEstCode;

}

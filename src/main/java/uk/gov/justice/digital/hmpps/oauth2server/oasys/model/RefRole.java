package uk.gov.justice.digital.hmpps.oauth2server.oasys.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Time;

@Data
@Entity
@Table(name = "REF_ROLE", schema = "EOR")
@NoArgsConstructor
@AllArgsConstructor
public class RefRole {
    @Column(name = "REF_ROLE_UK")
    private Long refRoleUk;
    @Id
    @Column(name = "REF_ROLE_CODE")
    private String refRoleCode;
    @Column(name = "REF_ROLE_DESC")
    private String refRoleDesc;
    @Column(name = "ROLE_TYPE_ELM")
    private String roleTypeElm;
    @Column(name = "ROLE_TYPE_CAT")
    private String roleTypeCat;
    @Column(name = "CT_AREA_EST_CODE")
    private String ctAreaEstCode;
    @Column(name = "RESTRICTED_IND")
    private String restrictedInd;
    @Column(name = "CHECKSUM")
    private String checksum;
    @Column(name = "CREATE_DATE")
    private Time createDate;
    @Column(name = "CREATE_USER")
    private String createUser;
    @Column(name = "LASTUPD_DATE")
    private Time lastupdDate;
    @Column(name = "LASTUPD_USER")
    private String lastupdUser;

}

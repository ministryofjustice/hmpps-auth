package uk.gov.justice.digital.hmpps.oauth2server.oasys.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Time;

@Data
@Entity
@Table(name = "CT_AREA_EST", schema = "EOR")
public class CtAreaEst {
    @Column(name = "CT_AREA_EST_UK")
    private Long ctAreaEstUk;
    @Id
    @Column(name = "CT_AREA_EST_CODE")
    private String ctAreaEstCode;
    @Column(name = "AREA_EST_CTID")
    private String areaEstCtid;
    @Column(name = "AREA_EST_NAME")
    private String areaEstName;
    @Column(name = "START_DATE")
    private Time startDate;
    @Column(name = "END_DATE")
    private Time endDate;
    @Column(name = "LAO_CONTACT_NUMBER")
    private String laoContactNumber;
    @Column(name = "SHOW_SDR_OBJ_IND")
    private String showSdrObjInd;
    @Column(name = "SHOW_SDR_HINTS_IND")
    private String showSdrHintsInd;
    @Column(name = "SHOW_CRIM_GRAPH_IND")
    private String showCrimGraphInd;
    @Column(name = "SDR_CONCERN_NONE_IND")
    private String sdrConcernNoneInd;
    @Column(name = "SDR_CONCERN_SUICIDE_IND")
    private String sdrConcernSuicideInd;
    @Column(name = "SDR_CONCERN_SELFHARM_IND")
    private String sdrConcernSelfharmInd;
    @Column(name = "SDR_CONCERN_CUSTODY_IND")
    private String sdrConcernCustodyInd;
    @Column(name = "SDR_CONCERN_HOSTEL_IND")
    private String sdrConcernHostelInd;
    @Column(name = "SDR_CONCERN_VULNERABLE_IND")
    private String sdrConcernVulnerableInd;
    @Column(name = "DATA_EXPORT_PATH")
    private String dataExportPath;
    @Column(name = "NOMIS_CODE")
    private String nomisCode;
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
    @Column(name = "PSR_LOGO_FILENAME")
    private String psrLogoFilename;
    @Column(name = "RUN_QA_SAMPLE_IND")
    private String runQaSampleInd;
    @Column(name = "BCS_ON_RECEPTION_IND")
    private String bcsOnReceptionInd;

}

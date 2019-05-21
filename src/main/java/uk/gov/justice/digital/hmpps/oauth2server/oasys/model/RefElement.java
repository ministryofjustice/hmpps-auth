package uk.gov.justice.digital.hmpps.oauth2server.oasys.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.sql.Time;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "REF_ELEMENT", schema = "EOR")
@IdClass(RefElementPK.class)
public class RefElement {
    @Column(name = "REF_ELEMENT_UK")
    private Long refElementUk;
    @Id
    @Column(name = "REF_CATEGORY_CODE")
    private String refCategoryCode;
    @Id
    @Column(name = "REF_ELEMENT_CODE")
    private String refElementCode;
    @Column(name = "REF_ELEMENT_CTID")
    private String refElementCtid;
    @Column(name = "REF_ELEMENT_SHORT_DESC")
    private String refElementShortDesc;
    @Column(name = "REF_ELEMENT_DESC")
    private String refElementDesc;
    @Column(name = "DISPLAY_SORT")
    private Long displaySort;
    @Column(name = "START_DATE")
    private Time startDate;
    @Column(name = "END_DATE")
    private Time endDate;
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

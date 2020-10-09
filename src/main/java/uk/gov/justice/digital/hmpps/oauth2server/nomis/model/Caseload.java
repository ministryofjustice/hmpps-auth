package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "CASELOADS")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "name"})
public class Caseload {

    @Id()
    @Column(name = "CASELOAD_ID", nullable = false)
    private String id;

    @Column(name = "DESCRIPTION", nullable = false)
    private String name;

}

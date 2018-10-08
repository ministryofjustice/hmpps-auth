package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;

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
@ToString(of = {"id", "name", "type"})
public class Caseload {

    @Id()
    @Column(name = "CASELOAD_ID", nullable = false)
    private String id;

    @Column(name = "DESCRIPTION", nullable = false)
    private String name;

    @Column(name = "CASELOAD_TYPE")
    private String type;

}

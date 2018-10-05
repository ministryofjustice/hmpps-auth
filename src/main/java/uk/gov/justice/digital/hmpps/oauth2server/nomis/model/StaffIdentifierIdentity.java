package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"staffId", "type", "identificationNumber"})
@Embeddable
@ToString(of = { "staffId", "type", "identificationNumber"})
public class StaffIdentifierIdentity implements Serializable {

    @Column(name = "STAFF_ID", nullable = false)
    private Long staffId;

    @Column(name = "IDENTIFICATION_TYPE", nullable = false)
    private String type;

    @Column(name = "IDENTIFICATION_NUMBER", nullable = false)
    private String identificationNumber;


}

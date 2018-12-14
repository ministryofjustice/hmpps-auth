package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"domain", "code"})
@ToString(of = {"domain", "code"})
public class DomainCodeIdentifier implements Serializable {
    @Column(name = "DOMAIN", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferenceDomain domain;

    @Column(name = "CODE", nullable = false)
    private String code;
}

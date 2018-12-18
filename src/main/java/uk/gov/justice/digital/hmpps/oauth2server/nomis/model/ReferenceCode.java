package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "REFERENCE_CODES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"domainCodeIdentifier"})
@ToString(of = {"domainCodeIdentifier", "description"})
public class ReferenceCode {

    @EmbeddedId
    private DomainCodeIdentifier domainCodeIdentifier;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Type(type = "yes_no")
    @Column(name = "ACTIVE_FLAG", nullable = false)
    private boolean active;

    @Column(name = "EXPIRED_DATE")
    private Date expiredDate;
}


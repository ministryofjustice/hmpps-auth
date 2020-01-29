package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "OMS_ROLES")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"code"})
@ToString(of = {"id", "code", "name"})
public class Role implements Serializable {

    @Id()
    @Column(name = "ROLE_ID", nullable = false)
    private Long id;

    @Column(name = "ROLE_CODE", nullable = false, unique = true)
    private String code;

    @Column(name = "ROLE_NAME")
    private String name;

}

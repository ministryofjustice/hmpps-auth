package uk.gov.justice.digital.hmpps.oauth2server.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "OMS_ROLES")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"code"})
public class Role {

    @Id()
    @Column(name = "ROLE_ID", nullable = false)
    private Long id;

    @Column(name = "ROLE_CODE", nullable = false, unique = true)
    private String code;

    @Column(name = "ROLE_NAME")
    private String name;

    @Column(name = "ROLE_SEQ", nullable = false)
    private int sequence;

    @ManyToOne
    @JoinColumn(name = "PARENT_ROLE_CODE", referencedColumnName = "ROLE_CODE")
    private Role parent;

}

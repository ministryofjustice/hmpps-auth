package uk.gov.justice.digital.hmpps.oauth2server.model;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "STAFF_MEMBERS")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"staffId"})
public class Staff {

    @Id()
    @Column(name = "STAFF_ID", nullable = false)
    private Long staffId;

    @Column(name = "FIRST_NAME", nullable = false)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false)
    private String lastName;

    @OneToMany(mappedBy = "staff")
    private List<StaffUserAccount> users;

}

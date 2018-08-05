package uk.gov.justice.digital.hmpps.oauth2server.model;

import lombok.*;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "STAFF_USER_ACCOUNTS")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"username"})
@ToString(of = { "username", "type"})
public class StaffUserAccount {

    @Id()
    @Column(name = "USERNAME", nullable = false)
    private String username;

    @ManyToOne
    @JoinColumn(name = "STAFF_ID")
    private Staff staff;

    @Column(name = "STAFF_USER_TYPE", nullable = false)
    private String type;

    @OneToMany
    @JoinColumn(name = "USERNAME")
    private List<UserCaseloadRole> roles;

    public List<UserCaseloadRole> filterRolesByCaseload(String caseload) {
        return roles.stream()
                .filter(r -> r.getId().getCaseload().equals(caseload))
                .collect(Collectors.toList());
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "STAFF_USER_ACCOUNTS")
@SecondaryTable(name = "SYS.USER$", pkJoinColumns = @PrimaryKeyJoinColumn(name = "NAME"))
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"username"})
@ToString(of = {"username", "type"})
public class StaffUserAccount {

    @Id()
    @Column(name = "USERNAME", nullable = false)
    private String username;

    @Column(name = "SPARE4", table = "SYS.USER$")
    private String password;

    @ManyToOne
    @JoinColumn(name = "STAFF_ID")
    private Staff staff;

    @Column(name = "STAFF_USER_TYPE", nullable = false)
    private String type;

    @OneToMany
    @JoinColumn(name = "USERNAME")
    private List<UserCaseloadRole> roles;

    @OneToMany
    @JoinColumn(name = "USERNAME")
    private List<UserAccessibleCaseload> caseloads;

    @OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private AccountDetail accountDetail;

    public List<UserCaseloadRole> filterRolesByCaseload(final String caseload) {
        return roles.stream()
                .filter(r -> r.getId().getCaseload().equals(caseload))
                .collect(Collectors.toList());
    }

    public List<UserAccessibleCaseload> filterByCaseload(final String caseload) {
        return caseloads.stream()
                .filter(r -> r.getId().getCaseload().equals(caseload))
                .collect(Collectors.toList());
    }
}

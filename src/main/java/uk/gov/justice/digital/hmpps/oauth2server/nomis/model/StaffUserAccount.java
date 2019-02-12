package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.*;

@Entity
@Table(name = "STAFF_USER_ACCOUNTS")
@SecondaryTable(name = "SYS.USER$", pkJoinColumns = @PrimaryKeyJoinColumn(name = "NAME"))
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"username"})
@ToString(of = {"username", "type"})
public class StaffUserAccount implements UserPersonDetails {

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

    @Column(name = "WORKING_CASELOAD_ID")
    private String activeCaseLoadId;

    @OneToMany(fetch = FetchType.EAGER)
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

    @Override
    public String getName() {
        return getStaff().getName();
    }

    @Override
    public String getFirstName() {
        return getStaff().getFirstName();
    }

    @Override
    public boolean isAdmin() {
        return accountDetail.getAccountProfile() == AccountProfile.TAG_ADMIN;
    }

    @Override
    public String getAuthSource() {
        return "nomis";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return filterRolesByCaseload("NWEB").stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + StringUtils.upperCase(role.getRole().getCode().replace('-', '_'))))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return EnumSet.of(OPEN, EXPIRED, EXPIRED_GRACE).contains(getAccountDetail().getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !EnumSet.of(EXPIRED, EXPIRED_LOCKED, EXPIRED_LOCKED_TIMED).contains(getAccountDetail().getStatus());
    }

    @Override
    public boolean isEnabled() {
        return EnumSet.of(OPEN, EXPIRED, EXPIRED_GRACE).contains(getAccountDetail().getStatus());
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }
}

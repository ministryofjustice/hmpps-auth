package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static javax.persistence.CascadeType.ALL;

@Entity
@Table(name = "GROUPS")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"groupCode"})
public class Group implements Serializable {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "group_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "group_code", nullable = false)
    private String groupCode;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @OneToMany(mappedBy = "group", cascade = ALL, orphanRemoval = true)
    private Set<GroupAssignableRole> assignableRoles = new HashSet<>();

    public Group(final String groupCode, final String groupName) {
        this.groupCode = groupCode;
        this.groupName = groupName;
    }

    public UUID getId() {
        return this.id;
    }

    public String getGroupCode() {
        return this.groupCode;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public Set<GroupAssignableRole> getAssignableRoles() {
        return this.assignableRoles;
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.GroupAssignableRole.GroupAssignableRoleId;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "GROUP_ASSIGNABLE_ROLE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"role", "automatic"})
@IdClass(GroupAssignableRoleId.class)
public class GroupAssignableRole implements Serializable {
    @Id
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Authority role;

    @Id
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private boolean automatic;

    public Authority getRole() {
        return this.role;
    }

    public Group getGroup() {
        return this.group;
    }

    public boolean isAutomatic() {
        return this.automatic;
    }

    public static class GroupAssignableRoleId implements Serializable {
        private UUID group;
        private UUID role;
    }
}

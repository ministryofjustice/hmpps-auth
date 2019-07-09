package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

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

    public Group(final String groupCode, final String groupName) {
        this.groupCode = groupCode;
        this.groupName = groupName;
    }
}

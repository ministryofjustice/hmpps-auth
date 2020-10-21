package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.text.WordUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "STAFF_MEMBERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"staffId"})
@ToString(of = {"staffId", "firstName", "lastName"})
public class Staff {

    private static final String STAFF_STATUS_ACTIVE = "ACTIVE";

    @Id()
    @Column(name = "STAFF_ID", nullable = false)
    private Long staffId;

    @Column(name = "FIRST_NAME", nullable = false)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false)
    private String lastName;

    @Column(name = "STATUS")
    private String status;

    public String getFirstName() {
        return WordUtils.capitalizeFully(firstName);
    }

    public String getName() {
        return WordUtils.capitalizeFully(String.format("%s %s", firstName, lastName));
    }

    public boolean isActive() {
        return STAFF_STATUS_ACTIVE.equals(status);
    }

    public Long getStaffId() {
        return this.staffId;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getStatus() {
        return this.status;
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import com.google.common.base.Splitter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "OAUTH_SERVICE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"code"})
public class Service {
    @Id
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "authorised_roles")
    private String authorisedRoles;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private boolean enabled;

    @Column
    private String email;

    public List<String> getRoles() {
        //noinspection UnstableApiUsage
        return StringUtils.isBlank(authorisedRoles) ? Collections.emptyList() : Splitter.on(',').trimResults().splitToList(authorisedRoles);
    }
}

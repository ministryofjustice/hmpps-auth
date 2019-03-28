package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "AUTHORITY")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"authority"})
public class Authority implements GrantedAuthority {
    private static final String ROLE_PREFIX = "ROLE_";

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "authority_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "authority", nullable = false)
    private String authority;

    public Authority(final String authority) {
        this.authority = addRolePrefixIfNecessary(authority);
    }

    public String getAuthorityName() {
        // strip off ROLE_
        return authority.substring(ROLE_PREFIX.length());
    }

    public static String addRolePrefixIfNecessary(final String role) {
        return StringUtils.startsWith(role, ROLE_PREFIX) ? role : ROLE_PREFIX + role;
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ROLES")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"roleCode"})
public class Authority implements GrantedAuthority {
    public static final String ROLE_PREFIX = "ROLE_";

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "role_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "role_code", nullable = false)
    private String roleCode;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    public Authority(final String roleCode, final String roleName) {
        this.roleCode = removeRolePrefixIfNecessary(roleCode);
        this.roleName = roleName;
    }

    public static String removeRolePrefixIfNecessary(final String role) {
        return StringUtils.startsWith(role, ROLE_PREFIX) ? StringUtils.substring(role, ROLE_PREFIX.length()) : role;
    }

    @Override
    public String getAuthority() {
        return ROLE_PREFIX + roleCode;
    }

    public UUID getId() {
        return this.id;
    }

    public String getRoleCode() {
        return this.roleCode;
    }

    public String getRoleName() {
        return this.roleName;
    }
}

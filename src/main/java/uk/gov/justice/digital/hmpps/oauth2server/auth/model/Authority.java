package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "authority_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "authority", nullable = false)
    private String authority;

    public Authority(final String authority) {
        this.authority = authority;
    }
}

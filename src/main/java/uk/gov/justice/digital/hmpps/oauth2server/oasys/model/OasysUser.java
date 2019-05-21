package uk.gov.justice.digital.hmpps.oauth2server.oasys.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.sql.Time;
import java.util.Collection;

@Data
@Entity
@Table(name = "OASYS_USER", schema = "EOR")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OasysUser implements UserPersonDetails {
    @Column(name = "OASYS_USER_UK")
    private Long oasysUserUk;
    @Id
    @Column(name = "OASYS_USER_CODE")
    private String oasysUserCode;
    @Column(name = "USER_FORENAME_1")
    private String userForename1;
    @Column(name = "USER_FORENAME_2")
    private String userForename2;
    @Column(name = "USER_FORENAME_3")
    private String userForename3;
    @Column(name = "USER_FAMILY_NAME")
    private String userFamilyName;
    @Column(name = "PASSWORD_ENCRYPTED")
    private byte[] passwordEncrypted;
    @Column(name = "PASSWORD_CHANGE_DATE")
    private Time passwordChangeDate;
    @Column(name = "LAST_LOGIN")
    private Time lastLogin;
    @Column(name = "FAILED_LOGIN_ATTEMPTS")
    private Long failedLoginAttempts;
    @Column(name = "SYSTEM_IND")
    private String systemInd;
    @OneToOne
    @JoinColumns({
            @JoinColumn(name = "USER_STATUS_CAT", referencedColumnName = "REF_CATEGORY_CODE"),
            @JoinColumn(name = "USER_STATUS_ELM", referencedColumnName = "REF_ELEMENT_CODE")
    })
    private RefElement userStatus;
    @Column(name = "DATE_OF_BIRTH")
    private Time dateOfBirth;
    @Column(name = "PASSWORD")
    private String password;
    @Column(name = "EMAIL_ADDRESS")
    private String emailAddress;
    @Column(name = "LEGACY_USER_CODE")
    private String legacyUserCode;
    @OneToOne
    @JoinColumns({
            @JoinColumn(name = "MIGRATION_SOURCE_CAT", referencedColumnName = "REF_CATEGORY_CODE"),
            @JoinColumn(name = "MIGRATION_SOURCE_ELM", referencedColumnName = "REF_ELEMENT_CODE")
    })
    private RefElement migrationSource;
    @Column(name = "CHECKSUM")
    private String checksum;
    @Column(name = "CREATE_DATE")
    private Time createDate;
    @Column(name = "CREATE_USER")
    private String createUser;
    @Column(name = "LASTUPD_DATE")
    private Time lastupdDate;
    @Column(name = "LASTUPD_USER")
    private String lastupdUser;
    @OneToOne
    @JoinColumn(name = "CT_AREA_EST_CODE")
    private CtAreaEst ctAreaEst;
    @Column(name = "EXCL_DEACT_IND")
    private String exclDeactInd;

    @Override
    public String getName() {
        return userFamilyName;
    }

    @Override
    public String getFirstName() {
        return userForename1;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public String getAuthSource() {
        return "oasys";
    }

    @Override
    public void eraseCredentials() {

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getUsername() {
        return oasysUserCode;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

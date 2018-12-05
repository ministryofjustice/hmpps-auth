package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus;

import javax.sql.DataSource;

@Component
@Slf4j
@Profile("oracle-auth & !oracle")
public class H2AuthenticationProvider extends AbstractAuthenticationProvider {

    private static final String GET_USER_DETAIL =
            "SELECT password as spare4, 0 as retry_count, account_status FROM dba_users v WHERE v.username = ?";
    private static final String UPDATE_STATUS = "UPDATE dba_users SET account_status = ? WHERE username = ?";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public H2AuthenticationProvider(@Qualifier("dataSource") final DataSource dataSource,
                                    @Qualifier("authDataSource") final DataSource authDataSource,
                                    final UserDetailsService userDetailsService) {
        super(userDetailsService, authDataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    protected UserData getUserData(final String username) {
        return jdbcTemplate.queryForObject(GET_USER_DETAIL, new Object[]{username}, new BeanPropertyRowMapper<>(H2UserData.class));
    }

    @Override
    protected void lockAccount(final AccountStatus status, final String username) {
        jdbcTemplate.update(UPDATE_STATUS, status.getDesc(), username);
    }

    @Override
    protected String encode(final String rawPassword, final String salt) {
        return BCrypt.hashpw(rawPassword, salt);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class H2UserData extends UserData {
        private String accountStatus;

        AccountStatus getStatus() {
            return AccountStatus.get(accountStatus);
        }

        String getSalt() {
            return getSpare4();
        }

        String getHash() {
            return getSpare4();
        }
    }
}

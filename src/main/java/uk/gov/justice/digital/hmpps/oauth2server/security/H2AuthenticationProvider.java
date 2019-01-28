package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Slf4j
@Profile("!oracle")
public class H2AuthenticationProvider extends AbstractAuthenticationProvider {

    private static final String GET_USER_DETAIL =
            "SELECT password as spare4, 0 as retry_count FROM dba_users v WHERE v.username = ?";
    private static final String UPDATE_STATUS = "UPDATE dba_users SET account_status = ? WHERE username = ?";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public H2AuthenticationProvider(@Qualifier("dataSource") final DataSource dataSource,
                                    final UserDetailsService userDetailsService,
                                    final UserRetriesService userRetriesService,
                                    final TelemetryClient telemetryClient,
                                    @Value("${application.authentication.lockout-count}") final int accountLockoutCount) {
        super(userDetailsService, userRetriesService, telemetryClient, accountLockoutCount);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    protected UserData getUserData(final String username) {
        final var results = jdbcTemplate.query(GET_USER_DETAIL, new Object[]{username}, new BeanPropertyRowMapper<>(H2UserData.class));
        return DataAccessUtils.singleResult(results);
    }

    @Override
    protected void lockAccount(final String username) {
        jdbcTemplate.update(UPDATE_STATUS, "LOCKED", username);
    }

    @Override
    protected String encode(final String rawPassword, final String salt) {
        return BCrypt.hashpw(rawPassword, salt);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class H2UserData extends UserData {
        String getSalt() {
            return getSpare4();
        }

        String getHash() {
            return getSpare4();
        }
    }
}

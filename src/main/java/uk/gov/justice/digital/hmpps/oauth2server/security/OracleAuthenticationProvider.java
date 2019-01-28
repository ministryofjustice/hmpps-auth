package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Slf4j
@Profile("oracle")
public class OracleAuthenticationProvider extends AbstractAuthenticationProvider {

    private static final String GET_USER_DETAIL = "SELECT spare4, LCOUNT retry_count, ASTATUS status_code FROM SYS.user$ WHERE name = ?";
    private static final String UPDATE_STATUS = "ALTER USER %s ACCOUNT LOCK";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OracleAuthenticationProvider(@Qualifier("dataSource") final DataSource dataSource,
                                        final UserDetailsService userDetailsService,
                                        final UserRetriesService userRetriesService,
                                        final TelemetryClient telemetryClient,
                                        @Value("${application.authentication.lockout-count}") final int accountLockoutCount) {
        super(userDetailsService, userRetriesService, telemetryClient, accountLockoutCount);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    protected UserData getUserData(final String username) {
        final var results = jdbcTemplate.query(GET_USER_DETAIL, new Object[]{username}, new BeanPropertyRowMapper<>(UserData.class));
        return DataAccessUtils.singleResult(results);
    }

    @Override
    protected void lockAccount(final String username) {
        jdbcTemplate.update(String.format(UPDATE_STATUS, username));
    }
}

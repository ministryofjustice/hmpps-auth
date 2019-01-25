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
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;

@Component
@Slf4j
@Profile("oracle")
public class OracleAuthenticationProvider extends AbstractAuthenticationProvider {

    private static final String GET_USER_DETAIL = "SELECT spare4, LCOUNT retry_count FROM SYS.user$ WHERE name = ?";
    private static final String HASH = "{ ? = call SYS.DBMS_CRYPTO.hash(UTL_RAW.cast_to_raw (?) || HEXTORAW (?), DBMS_CRYPTO.hash_sh1) }";
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

    protected String encode(final String rawPassword, final String salt) {
        final var params = List.of(
                new SqlOutParameter("encodedPassword", Types.VARCHAR),
                new SqlParameter(Types.VARCHAR),
                new SqlParameter(Types.VARCHAR));

        final var results = jdbcTemplate.call(
                con -> {
                    final CallableStatement cs = con.prepareCall(HASH);
                    cs.registerOutParameter(1, Types.VARCHAR);
                    cs.setString(2, rawPassword);
                    cs.setString(3, salt);
                    return cs;
                },
                params);

        return (String) results.get("encodedPassword");
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;

@Component
@Slf4j
@Profile("oracle-auth & oracle")
public class OracleAuthenticationProvider extends AbstractAuthenticationProvider {

    private static final String GET_USER_DETAIL = "SELECT spare4, LCOUNT retry_count, ASTATUS status_code FROM SYS.user$ WHERE name = ?";
    private static final String HASH = "{ ? = call SYS.DBMS_CRYPTO.hash(UTL_RAW.cast_to_raw (?) || HEXTORAW (?), DBMS_CRYPTO.hash_sh1) }";
    private static final String UPDATE_STATUS = "ALTER USER %s ACCOUNT LOCK";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OracleAuthenticationProvider(@Qualifier("dataSource") final DataSource dataSource,
                                        @Qualifier("authDataSource") final DataSource authDataSource,
                                        final UserDetailsService userDetailsService) {
        super(userDetailsService, authDataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    protected UserData getUserData(final String username) {
        return jdbcTemplate.queryForObject(GET_USER_DETAIL, new Object[]{username}, new BeanPropertyRowMapper<>(UserData.class));
    }

    @Override
    protected void lockAccount(final AccountStatus status, final String username) {
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

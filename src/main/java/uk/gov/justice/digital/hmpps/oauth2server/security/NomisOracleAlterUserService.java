package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import javax.sql.DataSource;
import java.sql.SQLException;

@Log4j2
@Service
@Profile("oracle")
public class NomisOracleAlterUserService extends NomisUserService {
    private static final String CHANGE_PASSWORD_SQL = "ALTER USER %s IDENTIFIED BY \"%s\"";
    private static final String CHANGE_PASSWORD_UNLOCK_SQL = CHANGE_PASSWORD_SQL + " ACCOUNT UNLOCK";
    private static final String UPDATE_STATUS = "ALTER USER %s ACCOUNT LOCK";

    private final JdbcTemplate jdbcTemplate;

    public NomisOracleAlterUserService(@Qualifier("dataSource") final DataSource dataSource,
                                       final StaffUserAccountRepository staffUserAccountRepository) {
        super(staffUserAccountRepository);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional
    @Override
    public void changePassword(final String username, final String password) {
        changePassword(username, password, CHANGE_PASSWORD_SQL);
    }

    @Transactional
    @Override
    public void changePasswordWithUnlock(final String username, final String password) {
        changePassword(username, password, CHANGE_PASSWORD_UNLOCK_SQL);
    }

    @Override
    public void lockAccount(final String username) {
        jdbcTemplate.update(String.format(UPDATE_STATUS, username));
    }

    private void changePassword(final String username, final String password, final String template) {
        try {
            jdbcTemplate.update(String.format(template, username, password));
        } catch (final DataAccessException e) {
            if (e.getCause() instanceof SQLException) {
                final var sqlException = (SQLException) e.getCause();
                if (sqlException.getErrorCode() == 28007) {
                    // password cannot be reused
                    log.info("Password cannot be reused exception caught: {}", sqlException.getMessage());
                    throw new ReusedPasswordException();
                }
                if (sqlException.getErrorCode() == 28003) {
                    // password validation failure - should be caught by the front end first
                    log.error("Password passed controller validation but failed oracle validation: {}",
                            sqlException.getMessage());
                    throw new PasswordValidationFailureException();
                }
            }
            log.error("Found error during changing password", e);
            throw e;
        }
    }
}

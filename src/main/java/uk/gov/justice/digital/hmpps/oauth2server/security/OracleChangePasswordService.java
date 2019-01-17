package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;

@Log4j2
@Service
@Profile("oracle")
public class OracleChangePasswordService implements ChangePasswordService {
    private final JdbcTemplate jdbcTemplate;

    public OracleChangePasswordService(@Qualifier("dataSource") final DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional
    public void changePassword(final String username, final String password) {
        try {
            jdbcTemplate.update(String.format("ALTER USER %s IDENTIFIED BY \"%s\"", username, password));
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

package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;

@Service
@Profile("oracle")
public class OracleUserService implements ChangePasswordService {
    private final JdbcTemplate jdbcTemplate;

    public OracleUserService(@Qualifier("dataSource") final DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional
    public void changePassword(final String username, final String password) {
        try {
            jdbcTemplate.update(String.format("ALTER USER %s IDENTIFIED BY %s", username, password));
        } catch (final DataAccessException e) {
            if (e.getCause() instanceof SQLException) {
                final var sqlException = (SQLException) e.getCause();
                if (sqlException.getErrorCode() == 28007) {
                    // password cannot be reused
                    throw new ReusedPasswordException();
                }
                if (sqlException.getErrorCode() == 28003) {
                    // password validation failure
                    throw new PasswordValidationFailureException();
                }
            }
            throw e;
        }
    }
}

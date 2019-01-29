package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Service
@Profile("!oracle")
public class H2AlterUserService implements AlterUserService {
    private static final String UPDATE_STATUS = "UPDATE dba_users SET account_status = ? WHERE username = ?";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder encoder;

    public H2AlterUserService(@Qualifier("dataSource") final DataSource dataSource,
                              final PasswordEncoder passwordEncoder) {
        jdbcTemplate = new JdbcTemplate(dataSource);
        this.encoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(final String username, final String password) {
        jdbcTemplate.update(String.format("ALTER USER %s SET PASSWORD ?", username), password);
        final var hashedPassword = encoder.encode(password);
        jdbcTemplate.update(UPDATE_STATUS, "OPEN", username);
        // also update h2 password table so that we have access to the hash.
        jdbcTemplate.update("UPDATE sys.user$ SET spare4 = ? WHERE name = ?", hashedPassword, username);
    }

    @Override
    public void changePasswordWithUnlock(final String username, final String password) {
        changePassword(username, password);
    }

    @Override
    public void lockAccount(final String username) {
        jdbcTemplate.update(UPDATE_STATUS, "LOCKED", username);
    }
}

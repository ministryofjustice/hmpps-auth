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
public class H2ChangePasswordService implements ChangePasswordService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder encoder;

    public H2ChangePasswordService(@Qualifier("dataSource") final DataSource dataSource,
                                   final PasswordEncoder passwordEncoder) {
        jdbcTemplate = new JdbcTemplate(dataSource);
        this.encoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(final String username, final String password) {
        jdbcTemplate.update(String.format("ALTER USER %s SET PASSWORD ?", username), password);
        // also update h2 password table so that we have access to the hash.
        final var hashedPassword = encoder.encode(password);
        jdbcTemplate.update("UPDATE dba_users SET password = ? where username = ?", hashedPassword, username);

    }
}

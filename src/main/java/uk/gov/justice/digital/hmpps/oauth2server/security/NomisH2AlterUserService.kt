package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import javax.sql.DataSource;

@Service
@Profile("!oracle")
public class NomisH2AlterUserService extends NomisUserService {
    @SuppressWarnings("SqlResolve")
    private static final String UPDATE_STATUS = "UPDATE dba_users SET account_status = ? WHERE username = ?";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder encoder;

    public NomisH2AlterUserService(@Qualifier("dataSource") final DataSource dataSource,
                                   final PasswordEncoder passwordEncoder,
                                   final StaffUserAccountRepository staffUserAccountRepository,
                                   final UserRepository userRepository) {
        super(staffUserAccountRepository, userRepository);
        jdbcTemplate = new JdbcTemplate(dataSource);
        encoder = passwordEncoder;
    }

    @SuppressWarnings("SqlResolve")
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

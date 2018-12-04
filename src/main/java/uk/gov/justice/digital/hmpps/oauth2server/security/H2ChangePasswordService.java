package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Service
@Profile("!oracle")
public class H2ChangePasswordService implements ChangePasswordService {
    private final JdbcTemplate jdbcTemplate;

    public H2ChangePasswordService(@Qualifier("dataSource") final DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional
    public void changePassword(final String username, final String password) {
        jdbcTemplate.update(String.format("ALTER USER %s SET PASSWORD ?", username), password);
    }
}

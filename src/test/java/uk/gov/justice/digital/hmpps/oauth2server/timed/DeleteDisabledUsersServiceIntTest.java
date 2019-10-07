package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig;
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("dev")
@Import({AuthDbConfig.class, NomisDbConfig.class, FlywayConfig.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Transactional(transactionManager = "authTransactionManager")
public class DeleteDisabledUsersServiceIntTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRetriesRepository userRetriesRepository;
    @Autowired
    @Qualifier("authDataSource")
    private DataSource dataSource;
    @Mock
    private TelemetryClient telemetryClient;

    private DeleteDisabledUsersService service;

    @Before
    public void setUp() {
        service = new DeleteDisabledUsersService(userRepository, userRetriesRepository, telemetryClient);
    }

    @Test
    public void findAndDeleteDisabledAuthUsers_Processed() {
        assertThat(service.processInBatches()).isEqualTo(3);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var jdbcTemplate = new JdbcTemplate(dataSource);
        final var whereClause = "where username in ('AUTH_DELETE', 'AUTH_DELETEALL', 'NOMIS_DELETE')";

        final var retries = jdbcTemplate.queryForList(String.format("select * from user_retries %s", whereClause));
        assertThat(retries).isEmpty();

        final var tokens = jdbcTemplate.queryForList(String.format("select * from user_token %s", whereClause));
        assertThat(tokens).isEmpty();

        final var users = jdbcTemplate.queryForList(String.format("select * from user_email %s", whereClause));
        assertThat(users).isEmpty();

        final var people = jdbcTemplate.queryForList(String.format("select * from person %s", whereClause));
        assertThat(people).isEmpty();

        final var roles = jdbcTemplate.queryForList(String.format("select * from user_email_roles %s", whereClause));
        assertThat(roles).isEmpty();

        final var groups = jdbcTemplate.queryForList(String.format("select * from user_email_groups %s", whereClause).replace("username", "useremail_username"));
        assertThat(groups).isEmpty();
    }
}

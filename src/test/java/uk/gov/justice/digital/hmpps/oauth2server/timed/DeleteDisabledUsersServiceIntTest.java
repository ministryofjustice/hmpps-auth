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
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
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
    private UserEmailRepository userEmailRepository;
    @Autowired
    private UserTokenRepository userTokenRepository;
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
        service = new DeleteDisabledUsersService(userEmailRepository, userRetriesRepository, userTokenRepository, telemetryClient);
    }

    @Test
    public void findAndDeleteDisabledAuthUsers_Processed() {
        assertThat(service.processInBatches()).isEqualTo(2);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var jdbcTemplate = new JdbcTemplate(dataSource);

        final var retries = jdbcTemplate.queryForList("select * from user_retries where username in ('AUTH_DELETE', 'AUTH_DELETEALL')");
        assertThat(retries).isEmpty();

        final var tokens = jdbcTemplate.queryForList("select * from user_token where username in ('AUTH_DELETE', 'AUTH_DELETEALL')");
        assertThat(tokens).isEmpty();

        final var users = jdbcTemplate.queryForList("select * from user_email where username in ('AUTH_DELETE', 'AUTH_DELETEALL')");
        assertThat(users).isEmpty();

        final var people = jdbcTemplate.queryForList("select * from person where username in ('AUTH_DELETE', 'AUTH_DELETEALL')");
        assertThat(people).isEmpty();

        final var roles = jdbcTemplate.queryForList("select * from user_email_roles where username in ('AUTH_DELETE', 'AUTH_DELETEALL')");
        assertThat(roles).isEmpty();

        final var groups = jdbcTemplate.queryForList("select * from user_email_groups where useremail_username in ('AUTH_DELETE', 'AUTH_DELETEALL')");
        assertThat(groups).isEmpty();
    }
}

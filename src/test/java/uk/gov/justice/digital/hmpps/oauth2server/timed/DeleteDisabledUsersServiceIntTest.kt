package uk.gov.justice.digital.hmpps.oauth2server.timed

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRetriesRepository
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig
import javax.sql.DataSource

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(transactionManager = "authTransactionManager")
class DeleteDisabledUsersServiceIntTest {
  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var userRetriesRepository: UserRetriesRepository

  @Autowired
  @Qualifier("authDataSource")
  private lateinit var dataSource: DataSource

  @Mock
  private lateinit var telemetryClient: TelemetryClient
  private lateinit var service: DeleteDisabledUsersService

  @BeforeEach
  fun setUp() {
    service = DeleteDisabledUsersService(userRepository, userRetriesRepository, telemetryClient)
  }

  @Test
  fun findAndDeleteDisabledAuthUsers_Processed() {
    val authDeleteId = userRepository.findByUsername("AUTH_DELETE").orElseThrow().id
    val authDeleteAllId = userRepository.findByUsername("AUTH_DELETEALL").orElseThrow().id
    val nomisDeleteId = userRepository.findByUsername("NOMIS_DELETE").orElseThrow().id
    assertThat(service.processInBatches()).isEqualTo(3)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
    val jdbcTemplate = JdbcTemplate(dataSource)
    val usernameWhereClause = "where username in ('AUTH_DELETE', 'AUTH_DELETEALL', 'NOMIS_DELETE')"
    val userIdWhereClause = String.format("where user_id in ('%s', '%s', '%s')", authDeleteId, authDeleteAllId, nomisDeleteId)
    val retries = jdbcTemplate.queryForList(String.format("select * from user_retries %s", usernameWhereClause))
    assertThat(retries).isEmpty()
    val users = jdbcTemplate.queryForList(String.format("select * from users %s", usernameWhereClause))
    assertThat(users).isEmpty()
    val tokens = jdbcTemplate.queryForList(String.format("select * from user_token %s", userIdWhereClause))
    assertThat(tokens).isEmpty()
    val roles = jdbcTemplate.queryForList(String.format("select * from user_role %s", userIdWhereClause))
    assertThat(roles).isEmpty()
    val groups = jdbcTemplate.queryForList(String.format("select * from user_group %s", userIdWhereClause))
    assertThat(groups).isEmpty()
  }
}

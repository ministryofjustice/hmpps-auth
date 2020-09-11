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

@Suppress("SqlResolve")
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
    val userIdWhereClause = "where user_id in ('$authDeleteId', '$authDeleteAllId', '$nomisDeleteId')"
    checkNoRows(jdbcTemplate, "user_retries", usernameWhereClause)
    checkNoRows(jdbcTemplate, "users", usernameWhereClause)
    checkNoRows(jdbcTemplate, "user_token", userIdWhereClause)
    checkNoRows(jdbcTemplate, "user_role", userIdWhereClause)
    checkNoRows(jdbcTemplate, "user_group", userIdWhereClause)
  }

  private fun checkNoRows(jdbcTemplate: JdbcTemplate, table: String, whereClause: String) {
    val retries = jdbcTemplate.queryForList("select * from $table $whereClause")
    assertThat(retries).isEmpty()
  }
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import java.time.LocalDateTime
import javax.sql.DataSource

@DataJpaTest
@ActiveProfiles("test")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(transactionManager = "authTransactionManager")
class UserTokenRepositoryTest {
  @Autowired
  private lateinit var repository: UserTokenRepository

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  @Qualifier("authDataSource")
  private lateinit var dataSource: DataSource

  @Test
  fun givenATransientEntityItCanBePersisted() {
    val user = transientUser()
    val entity = user.createToken(RESET)
    userRepository.save(user)
    TestTransaction.flagForCommit()
    TestTransaction.end()
    assertThat(entity.token).isNotNull()
    TestTransaction.start()
    val retrievedEntity = repository.findById(entity.token).orElseThrow()

    assertThat(retrievedEntity.token).isEqualTo(entity.token)
    assertThat(retrievedEntity.tokenType).isEqualTo(entity.tokenType)
    assertThat(retrievedEntity.tokenExpiry).isEqualTo(entity.tokenExpiry)
    assertThat(retrievedEntity.user).isEqualToIgnoringGivenFields(user, "tokens")
  }

  @Test
  fun givenAnExistingUserTheyCanBeRetrieved() {
    val retrievedEntity = repository.findById("reset").orElseThrow()
    assertThat(retrievedEntity.tokenType).isEqualTo(RESET)
    assertThat(retrievedEntity.tokenExpiry).isEqualTo(LocalDateTime.of(2018, 12, 10, 8, 55, 45))
    assertThat(retrievedEntity.user.username).isEqualTo("LOCKED_USER")
  }

  @Test
  fun `replacing existing token doesn't cause unique key violations`() {
    val retrievedEntity = repository.findById("reset4").orElseThrow()
    retrievedEntity.user.createToken(retrievedEntity.tokenType)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val jdbcTemplate = JdbcTemplate(dataSource)
    val list =
      jdbcTemplate.queryForList("select * from user_token where user_id = '${retrievedEntity.user.id}' and token_type = '${retrievedEntity.tokenType}'")
    assertThat(list).hasSize(1)
  }

  private fun transientUser(): User =
    createSampleUser(username = "userTokenRepository", email = "a@b.com", source = auth)
}

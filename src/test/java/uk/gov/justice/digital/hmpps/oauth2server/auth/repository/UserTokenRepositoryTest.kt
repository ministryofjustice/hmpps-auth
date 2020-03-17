package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.FlywayConfig
import uk.gov.justice.digital.hmpps.oauth2server.config.NomisDbConfig
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("dev")
@Import(AuthDbConfig::class, NomisDbConfig::class, FlywayConfig::class)
@AutoConfigureTestDatabase(replace = NONE)
@Transactional(transactionManager = "authTransactionManager")
class UserTokenRepositoryTest {
  @Autowired
  private lateinit var repository: UserTokenRepository

  @Autowired
  private lateinit var userRepository: UserRepository

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

    // equals only compares the business key columns
    assertThat(retrievedEntity).isEqualTo(entity)
    assertThat(retrievedEntity.token).isEqualTo(entity.token)
    assertThat(retrievedEntity.tokenType).isEqualTo(entity.tokenType)
    assertThat(retrievedEntity.tokenExpiry).isEqualTo(entity.tokenExpiry)
    assertThat(retrievedEntity.user).isEqualTo(user)
  }

  @Test
  fun givenAnExistingUserTheyCanBeRetrieved() {
    val retrievedEntity = repository.findById("reset").orElseThrow()
    assertThat(retrievedEntity.tokenType).isEqualTo(RESET)
    assertThat(retrievedEntity.tokenExpiry).isEqualTo(LocalDateTime.of(2018, 12, 10, 8, 55, 45))
    assertThat(retrievedEntity.user.username).isEqualTo("LOCKED_USER")
  }

  private fun transientUser(): User =
      User.builder().username("userTokenRepository").email("a@b.com").source(auth).build()
}

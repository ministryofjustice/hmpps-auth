package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface UserRepository : CrudRepository<User, UUID>, JpaSpecificationExecutor<User> {
  fun findByUsername(username: String?): Optional<User>
  fun findByUsernameAndSource(username: String?, source: AuthSource): Optional<User>
  fun findByUsernameAndMasterIsTrue(username: String?): Optional<User> =
    findByUsernameAndSource(username, AuthSource.auth)

  fun findByEmail(email: String?): List<User>
  fun findByEmailAndSourceOrderByUsername(email: String?, source: AuthSource): List<User>
  fun findByEmailAndMasterIsTrueOrderByUsername(username: String?): List<User> =
    findByEmailAndSourceOrderByUsername(username, AuthSource.auth)

  fun findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndSourceOrderByLastLoggedIn(
    lastLoggedIn: LocalDateTime,
    source: AuthSource
  ): List<User>

  fun findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndMasterIsTrueOrderByLastLoggedIn(lastLoggedIn: LocalDateTime): List<User> =
    findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndSourceOrderByLastLoggedIn(lastLoggedIn, AuthSource.auth)

  fun findTop10ByLastLoggedInBeforeAndEnabledIsFalseOrderByLastLoggedIn(lastLoggedIn: LocalDateTime): List<User>
  fun findByUsernameIn(usernames: List<String>): List<User>

  fun findTop10ByLastLoggedInBeforeAndEnabledIsTrueAndPreDisableWarningIsFalseAndVerifiedIsTrueAndSourceOrderByUsername(
    lastLoggedIn: LocalDateTime,
    source: AuthSource
  ): List<User>
}

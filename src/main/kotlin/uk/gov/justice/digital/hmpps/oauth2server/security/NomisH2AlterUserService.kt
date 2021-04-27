package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import javax.sql.DataSource

@Service
@Profile("!oracle")
class NomisH2AlterUserService(
  @Qualifier("dataSource") dataSource: DataSource,
  private val passwordEncoder: PasswordEncoder,
  staffUserAccountRepository: StaffUserAccountRepository,
  verifyEmailService: VerifyEmailService,
  userRepository: UserRepository
) : NomisUserService(staffUserAccountRepository, userRepository, verifyEmailService) {

  private val jdbcTemplate: JdbcTemplate = JdbcTemplate(dataSource)

  companion object {
    private const val UPDATE_STATUS = "UPDATE dba_users SET account_status = ? WHERE username = ?"
  }

  @Transactional
  override fun changePassword(username: String?, password: String?) {
    jdbcTemplate.update(String.format("ALTER USER %s SET PASSWORD ?", username), password)
    val hashedPassword = passwordEncoder.encode(password)
    jdbcTemplate.update(UPDATE_STATUS, "OPEN", username)
    // also update h2 password table so that we have access to the hash.
    jdbcTemplate.update("UPDATE sys.user$ SET spare4 = ? WHERE name = ?", hashedPassword, username)
  }

  override fun changePasswordWithUnlock(username: String?, password: String?) {
    changePassword(username, password)
  }

  override fun lockAccount(username: String?) {
    jdbcTemplate.update(UPDATE_STATUS, "LOCKED", username)
  }
}

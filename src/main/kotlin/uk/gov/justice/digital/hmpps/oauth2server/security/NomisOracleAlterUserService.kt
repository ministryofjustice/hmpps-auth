package uk.gov.justice.digital.hmpps.oauth2server.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.sql.SQLException
import javax.sql.DataSource

@Service
@Profile("oracle")
class NomisOracleAlterUserService(
  @Qualifier("dataSource") dataSource: DataSource,
  staffUserAccountRepository: StaffUserAccountRepository,
  userRepository: UserRepository,
  verifyEmailService: VerifyEmailService,
) : NomisUserService(staffUserAccountRepository, userRepository, verifyEmailService) {

  companion object {
    private const val CHANGE_PASSWORD_SQL = "ALTER USER %s IDENTIFIED BY \"%s\""
    private const val CHANGE_PASSWORD_UNLOCK_SQL = "$CHANGE_PASSWORD_SQL ACCOUNT UNLOCK"
    private const val UPDATE_STATUS = "ALTER USER %s ACCOUNT LOCK"
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val jdbcTemplate: JdbcTemplate = JdbcTemplate(dataSource)

  @Transactional
  override fun changePassword(username: String?, password: String?) {
    changePassword(username, password, CHANGE_PASSWORD_SQL)
  }

  @Transactional
  override fun changePasswordWithUnlock(username: String?, password: String?) {
    changePassword(username, password, CHANGE_PASSWORD_UNLOCK_SQL)
  }

  override fun lockAccount(username: String?) {
    jdbcTemplate.update(String.format(UPDATE_STATUS, username))
  }

  private fun changePassword(username: String?, password: String?, template: String) {
    try {
      jdbcTemplate.update(String.format(template, username, password))
    } catch (e: DataAccessException) {
      if (e.cause is SQLException) {
        val sqlException = e.cause as SQLException?
        if (sqlException!!.errorCode == 28007) {
          // password cannot be reused
          log.info("Password cannot be reused exception caught: {}", sqlException.message)
          throw ReusedPasswordException()
        }
        if (sqlException.errorCode == 28003) {
          // password validation failure - should be caught by the front end first
          log.error(
            "Password passed controller validation but failed oracle validation: {}",
            sqlException.message
          )
          throw PasswordValidationFailureException()
        }
      }
      log.error("Found error during changing password", e)
      throw e
    }
  }
}

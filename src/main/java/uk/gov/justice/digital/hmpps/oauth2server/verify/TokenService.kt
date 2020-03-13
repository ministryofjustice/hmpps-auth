package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.util.*
import javax.persistence.EntityNotFoundException

@Service
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
class TokenService(private val userTokenRepository: UserTokenRepository,
                   private val userService: UserService,
                   private val telemetryClient: TelemetryClient) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getToken(tokenType: TokenType, token: String): Optional<UserToken> {
    val userTokenOptional = userTokenRepository.findById(token)
    return userTokenOptional.filter { t -> t.tokenType == tokenType }
  }

  fun getUserFromToken(tokenType: TokenType, token: String): User {
    val userTokenOptional = userTokenRepository.findById(token)
    val userToken = userTokenOptional.filter { t -> t.tokenType == tokenType }.orElseThrow { EntityNotFoundException("Token not found $token") }
    return userToken.user
  }

  fun checkToken(tokenType: TokenType, token: String): Optional<String> {
    val userTokenOptional = getToken(tokenType, token)
    if (userTokenOptional.isEmpty) {
      log.info("Failed to {} due to invalid token", tokenType.description)
      telemetryClient.trackEvent("${tokenType.description}Failure",
          mapOf("reason" to "invalid"), null)
      return Optional.of("invalid")
    }
    val userToken = userTokenOptional.get()
    if (userToken.hasTokenExpired()) {
      log.info("Failed to {} due to expired token", tokenType.description)
      val username = userToken.user.username
      telemetryClient.trackEvent("${tokenType.description}Failure",
          mapOf("username" to username, "reason" to "expired"), null)
      return Optional.of("expired")
    }
    return Optional.empty()
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun createToken(tokenType: TokenType, username: String): String {
    log.info("Requesting {} for {}", tokenType.description, username)
    val user = userService.getOrCreateUser(username)
    val userToken = user.createToken(tokenType)
    telemetryClient.trackEvent("${tokenType.description}Request",
        mapOf("username" to username), null)
    return userToken.token
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun removeToken(tokenType: TokenType, token: String) =
      getToken(tokenType, token).ifPresent { userTokenRepository.delete(it) }
}

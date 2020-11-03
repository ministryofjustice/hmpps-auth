package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.NotificationClientRuntimeException
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import javax.persistence.EntityNotFoundException

@Service
@Transactional(transactionManager = "authTransactionManager")
class InitialPasswordService(
  private val userRepository: UserRepository,
  private val oauthServiceRepository: OauthServiceRepository,
  private val userService: UserService,
  private val notificationClient: NotificationClientApi,
  @Value("\${application.notify.create-initial-password-resend.template}") private val initialPasswordResendTemplateId: String,
  private val telemetryClient: TelemetryClient,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(transactionManager = "authTransactionManager")
  fun resendInitialPasswordLink(username: String, url: String): String {
    val user: User = userRepository.findByUsername(username.toUpperCase())
      .orElseThrow { EntityNotFoundException(String.format("User not found with username %s", username)) }
    val userDetails: UserPersonDetails
    userDetails = if (user.isMaster) {
      user
    } else {
      userService.findMasterUserPersonDetails(user.username)
        .orElseThrow { EntityNotFoundException(String.format("User not found with username %s", username)) }
    }
    val userToken = user.createToken(UserToken.TokenType.RESET)
    val shortenedUrl = url.replace("-expired", "")
    val resetLink = String.format("%s?token=%s", shortenedUrl, userToken.token)
    val supportLink: String = getInitialEmailSupportLink(userDetails.toUser().groups)
    val parameters =
      mapOf(
        "firstName" to userDetails.firstName,
        "fullName" to userDetails.name,
        "supportLink" to supportLink,
        "resetLink" to resetLink
      )
    sendEmail(user.username, initialPasswordResendTemplateId, parameters, user.email)
    return resetLink
  }

  private fun getInitialEmailSupportLink(groups: Collection<Group>): String {
    val serviceCode = groups.firstOrNull { it.groupCode.startsWith("PECS") }?.let { "BOOK_MOVE" } ?: "NOMIS"
    return oauthServiceRepository.findById(serviceCode).map { it.email!! }.orElseThrow()
  }

  private fun sendEmail(username: String, template: String, parameters: Map<String, String>, email: String) {
    try {
      log.info("Sending reset password to notify for user {}", username)
      notificationClient.sendEmail(template, email, parameters, null)
      telemetryClient.trackEvent("reissueInitialPasswordLink", mapOf("username" to username), null)
    } catch (e: NotificationClientException) {
      log.warn("Failed to send reissue initial password to notify for user {}", username, e)
      if (e.httpResult >= 500) {
        // second time lucky
        try {
          notificationClient.sendEmail(template, email, parameters, null)
          telemetryClient.trackEvent("reissueInitialPasswordLink", mapOf("username" to username), null)
        } catch (e1: NotificationClientException) {
          throw NotificationClientRuntimeException(e1)
        }
      }
      throw NotificationClientRuntimeException(e)
    }
  }
}

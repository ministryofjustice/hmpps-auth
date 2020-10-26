package uk.gov.justice.digital.hmpps.oauth2server.verify

import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.NotificationClientRuntimeException
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.ResetPasswordException
import java.util.Optional

interface ResetPasswordService : PasswordService {
  @Throws(NotificationClientRuntimeException::class)
  fun requestResetPassword(usernameOrEmailAddress: String, url: String): Optional<String>
  override fun setPassword(token: String, password: String?)

  @Throws(ResetPasswordException::class)
  fun moveTokenToAccount(token: String, username: String?): String
}

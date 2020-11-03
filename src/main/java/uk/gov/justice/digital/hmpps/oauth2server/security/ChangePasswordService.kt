package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.authentication.LockedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.PasswordService

@Service

@Transactional

class ChangePasswordService(
  private val userTokenRepository: UserTokenRepository,
  private val userRepository: UserRepository,
  private val userService: UserService,
  private val delegatingUserService: DelegatingUserService,
) : PasswordService {

  @Transactional(transactionManager = "authTransactionManager")
  override fun setPassword(token: String, password: String?) {
    val userToken = userTokenRepository.findById(token).orElseThrow()
    val user = userToken.user
    val userPersonDetails =
      if (user.isMaster) user else userService.findMasterUserPersonDetails(user.username).orElseThrow()

    // before we set, ensure user allowed to still change their password
    if (!userPersonDetails.isEnabled || !userPersonDetails.isAccountNonLocked) {
      // failed, so let user know
      throw LockedException("locked")
    }
    delegatingUserService.changePassword(userPersonDetails, password!!)
    user.removeToken(userToken)
    userRepository.save(user)
  }
}

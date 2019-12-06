package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

@Service
open class DelegatingUserService(
    private val nomisUserService: NomisUserService,
    private val authUserService: AuthUserService,
    private val deliusUserService: DeliusUserService) {

  open fun lockAccount(userPersonDetails: UserPersonDetails) {
    // need to lock the user in auth too
    authUserService.lockUser(userPersonDetails)

    when (userPersonDetails) {
      is NomisUserPersonDetails -> nomisUserService.lockAccount(userPersonDetails.username)
      // don't lock the delius account, they will have x retries left there so will be handled by ldap anyway
    }
  }

  open fun changePasswordWithUnlock(userPersonDetails: UserPersonDetails, password: String?) {
    // need to unlock the user in auth too
    authUserService.unlockUser(userPersonDetails)

    when (userPersonDetails.authSource) {
      "auth" -> authUserService.changePassword(userPersonDetails as User, password)
      "nomis" -> nomisUserService.changePasswordWithUnlock(userPersonDetails.username, password)
      "delius" -> deliusUserService.changePassword(userPersonDetails.username, password)
    }
  }

  open fun changePassword(userPersonDetails: UserPersonDetails, password: String?) {
    when (userPersonDetails.authSource) {
      "auth" -> authUserService.changePassword(userPersonDetails as User, password)
      "nomis" -> nomisUserService.changePassword(userPersonDetails.username, password)
      "delius" -> deliusUserService.changePassword(userPersonDetails.username, password)
    }
  }
}

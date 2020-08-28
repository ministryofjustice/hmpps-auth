@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.util.OAuth2Utils
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.security.oauth2.provider.approval.TokenStoreUserApprovalHandler

class UserContextApprovalHandler : TokenStoreUserApprovalHandler() {
  private val approvalParameter = OAuth2Utils.USER_OAUTH_APPROVAL

  override fun checkForPreApproval(authorizationRequest: AuthorizationRequest, userAuthentication: Authentication): AuthorizationRequest {
    // Client needs to be setup so that scopes include the required user accounts, but auto approve scopes exclude the
    // required user accounts.  This will force us down the pre approval route.
    val approvalRequest = super.checkForPreApproval(authorizationRequest, userAuthentication)
    // if approved then good to go
    if (approvalRequest.isApproved()) {
      return approvalRequest
    }
    // So now must have at least one special non auto approval scope e.g. delius
    // TODO: Grab all the user's accounts for the scopes and if only one then can still auto approve
    //       For now we require manual approval instead
    return approvalRequest
  }
}

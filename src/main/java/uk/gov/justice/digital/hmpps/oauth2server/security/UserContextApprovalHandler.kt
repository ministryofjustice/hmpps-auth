@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.security.oauth2.provider.approval.TokenStoreUserApprovalHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService

class UserContextApprovalHandler(private val userContextService: UserContextService) : TokenStoreUserApprovalHandler() {
  override fun checkForPreApproval(
    authorizationRequest: AuthorizationRequest,
    userAuthentication: Authentication,
  ): AuthorizationRequest {

    val approvalRequest = super.checkForPreApproval(authorizationRequest, userAuthentication)
    if (approvalRequest.isApproved && isAzureAdUser(userAuthentication)) {
      // force all azure users down the approval route, the controller will work out what accounts are found etc.
      approvalRequest.isApproved = false
    }

    return approvalRequest
  }

  private fun isAzureAdUser(userAuthentication: Authentication) =
    AuthSource.fromNullableString((userAuthentication.principal as UserDetailsImpl).authSource) == azuread

  override fun getUserApprovalRequest(
    authorizationRequest: AuthorizationRequest,
    userAuthentication: Authentication,
  ): MutableMap<String, Any> {

    val userApprovalRequest = super.getUserApprovalRequest(authorizationRequest, userAuthentication)
    val userDetails = userAuthentication.principal as UserPersonDetails
    val users = userContextService.discoverUsers(userDetails, authorizationRequest.scope)
    userApprovalRequest["users"] = users

    return userApprovalRequest
  }
}

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

    // we have hijacked the UserContextApprovalHandler for our account selection process.
    // we are purposefully not calling the super method, because if we deny the request
    // based on unapproved scopes we do not currently have a way to explicitly approve it.

    // for now, all Azure AD users are sent down this route.
    // the controller will work out what accounts are found etc.
    authorizationRequest.isApproved = !isAzureAdUser(userAuthentication)

    return authorizationRequest
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

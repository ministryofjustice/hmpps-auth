@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.oauth2.provider.approval.TokenStoreUserApprovalHandler
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.azuread
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService

class UserContextApprovalHandler(
  private val userContextService: UserContextService,
  private val clientDetailsService: ClientDetailsService,
  private val mfaService: MfaService,
) : TokenStoreUserApprovalHandler() {

  init {
    super.setClientDetailsService(clientDetailsService)
  }

  /**
   * Users need approval if:
   * <ol>
   *   <li>The service requires MFA and they haven't already been through it</li>
   *   <li>The user is an Azure user</li>
   * </ol>
   */
  override fun checkForPreApproval(
    authorizationRequest: AuthorizationRequest,
    userAuthentication: Authentication,
  ): AuthorizationRequest {

    // we have hijacked the UserContextApprovalHandler for our account selection process.
    // we are purposefully not calling the super method, because if we deny the request
    // based on unapproved scopes we do not currently have a way to explicitly approve it.

    if (!(userAuthentication.principal as UserDetailsImpl).passedMfa && clientNeedsMfa(authorizationRequest.clientId)) {
      authorizationRequest.isApproved = false
      return authorizationRequest
    }

    // All Azure AD users are sent down this route, the controller will work out what accounts are found etc.
    authorizationRequest.isApproved = !isAzureAdUser(userAuthentication)

    return authorizationRequest
  }

  private fun isAzureAdUser(userAuthentication: Authentication) =
    AuthSource.fromNullableString((userAuthentication.principal as UserDetailsImpl).authSource) == azuread

  /**
   * Overridden to
   * <ol>
   *   <li>Store whether the user needs MFA in the approval request</li>
   *   <li>Retrieve and store azure users in the approval request for use by the authorization endpoint</li>
   * </ol>
   */
  override fun getUserApprovalRequest(
    authorizationRequest: AuthorizationRequest,
    userAuthentication: Authentication,
  ): MutableMap<String, Any> {

    val userApprovalRequest = super.getUserApprovalRequest(authorizationRequest, userAuthentication)

    // if we are in as an azure user find out any users that can be mapped to the current user
    val userDetails = userAuthentication.principal as UserDetailsImpl
    if (userDetails.authSource == azuread.source) {
      val users = userContextService.discoverUsers(userDetails, authorizationRequest.scope)
      userApprovalRequest["users"] = users
    }

    if (!userDetails.passedMfa && clientNeedsMfa(authorizationRequest.clientId)) {
      // found a client that requires mfa
      userApprovalRequest["requireMfa"] = true
    }

    return userApprovalRequest
  }

  private fun clientNeedsMfa(clientId: String): Boolean {
    val client = clientDetailsService.loadClientByClientId(clientId)
    val mfa = client.additionalInformation["mfa"] as? String?
    return (mfa == MfaAccess.untrusted.name && mfaService.outsideApprovedNetwork()) || mfa == MfaAccess.all.name
  }
}

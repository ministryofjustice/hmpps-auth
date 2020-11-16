@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.security.oauth2.provider.token.TokenEnhancer
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.Companion.fromNullableString
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import java.util.Optional

class JWTTokenEnhancer : TokenEnhancer {
  @Autowired
  private lateinit var clientsDetailsService: JdbcClientDetailsService

  companion object {
    private const val ADD_INFO_AUTH_SOURCE = "auth_source"
    const val ADD_INFO_NAME = "name"
    const val ADD_INFO_USER_NAME = "user_name"
    const val ADD_INFO_USER_ID = "user_id"
    const val SUBJECT = "sub"
    private const val REQUEST_PARAM_USER_NAME = "username"
    private const val REQUEST_PARAM_AUTH_SOURCE = "auth_source"
    private const val LEGACY_USERNAME = "legacy_username"
  }

  override fun enhance(accessToken: OAuth2AccessToken, authentication: OAuth2Authentication): OAuth2AccessToken {
    val additionalInfo = if (authentication.isClientOnly) {
      addUserFromExternalId(authentication)
    } else {
      val userAuthentication = authentication.userAuthentication
      val userDetails = userAuthentication.principal as UserPersonDetails
      val userId = StringUtils.defaultString(userDetails.userId, userAuthentication.name)
      val clientDetails = clientsDetailsService.loadClientByClientId(authentication.oAuth2Request.clientId)
      // note that DefaultUserAuthenticationConverter will automatically add user_name to the access token, so
      // removal of user_name will only affect the authorisation code response and not the access token field.
      filterAdditionalInfo(
        mapOf<String, Any>(
          SUBJECT to userAuthentication.name,
          ADD_INFO_AUTH_SOURCE to StringUtils.defaultIfBlank(userDetails.authSource, "none"),
          ADD_INFO_USER_NAME to userAuthentication.name,
          ADD_INFO_USER_ID to userId,
          ADD_INFO_NAME to userDetails.name
        ),
        clientDetails
      )
    }
    (accessToken as DefaultOAuth2AccessToken).additionalInformation = additionalInfo
    return accessToken
  }

  private fun filterAdditionalInfo(info: Map<String, Any>, clientDetails: ClientDetails): Map<String, Any> {
    val jwtFields = clientDetails.additionalInformation.getOrDefault("jwtFields", "") as String
    val entries = if (StringUtils.isBlank(jwtFields)) emptySet()
    else jwtFields.split(",").associateBy({ it.substring(1) }, { it[0] == '+' }).entries

    val fieldsToKeep = entries.filter { it.value }.map { it.key }.toSet()
    val fieldsToRemove = entries.filterNot { it.value }.map { it.key }.toMutableSet()

    // for field addition, just remove from deprecated fields
    fieldsToRemove.remove(fieldsToKeep)
    return info.entries.filterNot { fieldsToRemove.contains(it.key) }.associateBy({ it.key }, { it.value })
  }

  // Checks for existence of request parameters that define an external user identifier type and identifier.
  // If both identifier type and identifier parameters are present, they will be used to attempt to identify
  // a system user account. If a system user account is not identified, an exception will be thrown to ensure
  // authentication fails. If a system user account is identified, the user id will be added to the token,
  // the token's scope will be 'narrowed' to include 'write' scope and the system user's roles will be added
  // to token authorities.
  private fun addUserFromExternalId(authentication: OAuth2Authentication): Map<String, Any> {
    val additionalInfo: MutableMap<String, Any> = mutableMapOf()

    // Determine if both user_id_type and user_id request parameters exist.
    val request = authentication.oAuth2Request
    val requestParams = request.requestParameters
    val username = getUsernameFromRequestParam(requestParams)
    if (username.isPresent) {
      additionalInfo[ADD_INFO_USER_NAME] = username.get()
      additionalInfo[SUBJECT] = username.get()
    } else {
      additionalInfo[SUBJECT] = authentication.name
    }
    additionalInfo[ADD_INFO_AUTH_SOURCE] = getAuthSourceFromRequestParam(requestParams)

    val clientDetails = clientsDetailsService.loadClientByClientId(authentication.oAuth2Request.clientId)
    clientDetails.additionalInformation.get("legacyUsernameField")?.let {
      additionalInfo[LEGACY_USERNAME] = it as String
    }

    return additionalInfo
  }

  private fun getAuthSourceFromRequestParam(requestParams: Map<String, String>): String = try {
    fromNullableString(requestParams[REQUEST_PARAM_AUTH_SOURCE]).source
  } catch (iae: IllegalArgumentException) {
    AuthSource.none.source
  }

  private fun getUsernameFromRequestParam(requestParams: Map<String, String>): Optional<String> {
    if (requestParams.containsKey(REQUEST_PARAM_USER_NAME)) {
      val username = StringUtils.upperCase(requestParams[REQUEST_PARAM_USER_NAME])
      if (StringUtils.isNotBlank(username)) {
        return Optional.of(username)
      }
    }
    return Optional.empty()
  }
}

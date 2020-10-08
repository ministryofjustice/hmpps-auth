package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.azure.AzureUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.io.IOException
import java.util.ArrayList
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class OidcJwtAuthenticationSuccessHandler(jwtCookieHelper: JwtCookieHelper?,
                                          jwtAuthenticationHelper: JwtAuthenticationHelper?,
                                          cookieRequestCache: CookieRequestCache?,
                                          verifyEmailService: VerifyEmailService?,
                                          @Qualifier("tokenVerificationApiRestTemplate") restTemplate: RestTemplate?,
                                          @Value("\${tokenverification.enabled:false}") tokenVerificationEnabled: Boolean,
                                          private val userRetriesService: UserRetriesService) : JwtAuthenticationSuccessHandler(jwtCookieHelper, jwtAuthenticationHelper, cookieRequestCache, verifyEmailService, restTemplate, tokenVerificationEnabled) {
  @Throws(IOException::class, ServletException::class)
  override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse,
                                       authentication: Authentication) {
    val oidcUser = authentication.principal
    if (oidcUser is DefaultOidcUser) {
      userRetriesService.resetRetriesAndRecordLogin(constructAzureUserPersonDetails(oidcUser))
      super.onAuthenticationSuccess(request, response, authentication)
    } else {
      throw RuntimeException("Expected a DefaultOidcUser - is the Azure OIDC configuration correct?")
    }
  }

  private fun constructAzureUserPersonDetails(principal: DefaultOidcUser): AzureUserPersonDetails {
    // Enables compatibility with dev and production Azure configurations
    val givenName = when {
      principal.givenName != null -> principal.givenName
      principal.fullName.split(",").size == 2 -> principal.fullName.split(",")[1].trim()
      else -> ""
    }

    val familyName = when {
      principal.familyName != null -> principal.familyName
      principal.fullName.split(",").size == 2 -> principal.fullName.split(",")[0].trim()
      else -> ""
    }

    return AzureUserPersonDetails(
        ArrayList(),
        true,
        principal.getClaim<String>("oid").toUpperCase(),
        givenName,
        familyName,
        principal.preferredUsername.toLowerCase(),
        true,
        accountNonExpired = true,
        accountNonLocked = true)
  }
}

package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.azure.AzureUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.io.IOException
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
        val authenticationResult = authentication as OAuth2AuthenticationToken
        val principal = authenticationResult.principal as DefaultOidcUser

        // Enables compatibility with dev and production Azure configurations
        val givenName = when {
            principal.givenName != null -> principal.givenName
            principal.fullName.split(",").size == 2 -> principal.fullName.split(",")[1]
            else -> ""
        }

        val familyName = when {
            principal.familyName != null -> principal.familyName
            principal.fullName.split(",").size == 2 -> principal.fullName.split(",")[0]
            else -> ""
        }

        val upd = AzureUserPersonDetails(ArrayList(),
                true,
                principal.name,
                givenName,
                familyName,
                principal.preferredUsername,
                true,
                "",
                accountNonExpired = true,
                accountNonLocked = true)

        userRetriesService.resetRetriesAndRecordLogin(upd)
        super.onAuthenticationSuccess(request, response, authentication)
    }

}

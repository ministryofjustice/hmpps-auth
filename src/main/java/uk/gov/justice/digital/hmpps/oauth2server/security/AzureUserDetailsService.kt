package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.azure.AzureUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.azure.service.AzureUserService

@Service("azureUserDetailsService")
class AzureUserDetailsService(private val azureUserService: AzureUserService) :
    UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
  override fun loadUserByUsername(username: String): AzureUserPersonDetails {
    return azureUserService.getAzureUserByUsername(username)
        .orElseThrow { UsernameNotFoundException(username) }
  }

  override fun loadUserDetails(token: PreAuthenticatedAuthenticationToken): UserDetails = loadUserByUsername(token.name)
}

package uk.gov.justice.digital.hmpps.oauth2server.security

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService

@Service("deliusUserDetailsService")
@Transactional(readOnly = true)
open class DeliusUserDetailsService(private val deliusUserService: DeliusUserService,
                                    private val userService: UserService) :
    UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

  override fun loadUserByUsername(username: String): UserDetails {
    // need to check first that the user hasn't been locked in auth - as we are handling locking on behalf of delius
    val locked = userService.findUser(username).map { !it.isAccountNonLocked }.orElse(false)

    return deliusUserService.getDeliusUserByUsername(username)
        .map { it.copy(locked = locked) }
        .orElseThrow { UsernameNotFoundException(username) }
  }

  override fun loadUserDetails(token: PreAuthenticatedAuthenticationToken): UserDetails = loadUserByUsername(token.name)
}

@Component
@Transactional(readOnly = true, noRollbackFor = [BadCredentialsException::class])
open class DeliusAuthenticationProvider(private val deliusUserService: DeliusUserService,
                                        deliusUserDetailsService: DeliusUserDetailsService,
                                        userRetriesService: UserRetriesService,
                                        telemetryClient: TelemetryClient,
                                        @Value("\${application.authentication.lockout-count}") accountLockoutCount: Int) :
    LockingAuthenticationProvider(deliusUserDetailsService, userRetriesService, telemetryClient, accountLockoutCount) {


  override fun checkPassword(userDetails: UserDetails, password: String): Boolean =
      deliusUserService.authenticateUser(userDetails.username, password)
}

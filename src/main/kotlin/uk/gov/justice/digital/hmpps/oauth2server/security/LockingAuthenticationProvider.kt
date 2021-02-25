package uk.gov.justice.digital.hmpps.oauth2server.security

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.AccountStatusException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsChecker
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaClientNetworkService

abstract class LockingAuthenticationProvider(
  userDetailsService: UserDetailsService,
  private val userRetriesService: UserRetriesService,
  private val mfaClientNetworkService: MfaClientNetworkService,
  private val userService: UserService,
  private val telemetryClient: TelemetryClient
) : DaoAuthenticationProvider() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  init {
    @Suppress("LeakingThis")
    setUserDetailsService(userDetailsService)
    val oracleSha1PasswordEncoder = OracleSha1PasswordEncoder()
    val encoders = mapOf("bcrypt" to BCryptPasswordEncoder(), "oracle" to oracleSha1PasswordEncoder)
    val delegatingPasswordEncoder = DelegatingPasswordEncoder("bcrypt", encoders)
    delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(oracleSha1PasswordEncoder)
    passwordEncoder = delegatingPasswordEncoder
    preAuthenticationChecks = PreAuthenticationChecks()
  }

  private inner class PreAuthenticationChecks : UserDetailsChecker {

    override fun check(user: UserDetails) {
      if (!user.isAccountNonLocked) {
        throw LockedException(
          messages.getMessage(
            "AbstractUserDetailsAuthenticationProvider.locked",
            "User account is locked"
          )
        )
      }
      if (!user.isEnabled) {
        throw NextProviderDisabledException(
          messages.getMessage(
            "AbstractUserDetailsAuthenticationProvider.disabled",
            "User is disabled"
          )
        )
      }
      if (!user.isAccountNonExpired) {
        throw AccountExpiredException(
          messages.getMessage(
            "AbstractUserDetailsAuthenticationProvider.expired",
            "User account has expired"
          )
        )
      }
    }
  }

  @Throws(AuthenticationException::class)
  override fun authenticate(authentication: Authentication): Authentication {
    if (authentication.name.isNullOrBlank() || authentication.credentials == null ||
      authentication.credentials.toString().isBlank()
    ) {
      log.info("Credentials missing for user {}", authentication.name)
      throw MissingCredentialsException()
    }
    return try {
      val fullAuthentication = super.authenticate(authentication)
      val userDetails = fullAuthentication.principal as UserPersonDetails

      // now check if mfa is enabled for the user
      if (mfaClientNetworkService.needsMfa(fullAuthentication.authorities)) {
        if (userService.hasVerifiedMfaMethod(userDetails)) {
          throw MfaRequiredException("MFA required")
        }
        throw MfaUnavailableException("MFA required, but no email set")
      }
      val username = userDetails.username
      log.info("Successful login for user {}", username)
      telemetryClient.trackEvent("AuthenticateSuccess", mapOf("username" to username), null)
      fullAuthentication
    } catch (e: AuthenticationException) {
      val reason = e.javaClass.simpleName
      val username = authentication.name
      log.info(
        "Authenticate failed for user {} with reason {} and message {}",
        username,
        reason,
        e.message
      )
      throw e
    }
  }

  @Throws(AuthenticationException::class)
  override fun additionalAuthenticationChecks(
    userDetails: UserDetails,
    authentication: UsernamePasswordAuthenticationToken
  ) {
    val password = authentication.credentials.toString()
    checkPasswordWithAccountLock(userDetails as UserPersonDetails, password)
  }

  private fun checkPasswordWithAccountLock(userDetails: UserPersonDetails, password: String) {
    val username = userDetails.username
    if (checkPassword(userDetails, password)) {
      log.info("Resetting retries for user {}", username)
      userRetriesService.resetRetriesAndRecordLogin(userDetails)
    } else {
      val locked = userRetriesService.incrementRetriesAndLockAccountIfNecessary(userDetails)

      // check the number of retries
      if (locked) {
        log.info("Locked account for user {}", username)
        throw LockedException("Account is locked, number of retries exceeded")
      }
      log.info("Credentials incorrect for user {}", username)
      throw BadCredentialsException("Authentication failed: password does not match stored value")
    }
  }

  protected open fun checkPassword(userDetails: UserDetails, password: String): Boolean =
    passwordEncoder.matches(password, userDetails.password)

  class MfaRequiredException(msg: String) : AccountStatusException(msg)
  class MfaUnavailableException(msg: String) : AccountStatusException(msg)
  private inner class NextProviderDisabledException(msg: String) : AuthenticationException(msg)
}

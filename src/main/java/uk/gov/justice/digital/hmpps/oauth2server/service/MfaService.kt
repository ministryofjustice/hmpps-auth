package uk.gov.justice.digital.hmpps.oauth2server.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService
import java.util.*

@Service
open class MfaService(@Value("\${application.authentication.mfa.whitelist}") whitelist: Set<String>,
                      @Value("\${application.authentication.mfa.roles}") private val mfaRoles: Set<String>,
                      private val tokenService: TokenService) {
  private val ipMatchers: List<IpAddressMatcher>

  init {
    ipMatchers = whitelist.map { ip -> IpAddressMatcher(ip) }
  }

  open fun needsMfa(authorities: Collection<GrantedAuthority>): Boolean {
    // if they're whitelisted then no mfa
    val ip = IpAddressHelper.retrieveIpFromRequest()
    return if (ipMatchers.any { it.matches(ip) }) {
      false
      // otherwise check that they have a role that requires mfa
    } else authorities.stream().map { it.authority }.anyMatch { r -> mfaRoles.contains(r) }
  }

  @Transactional(transactionManager = "authTransactionManager")
  open fun createTokenAndSendEmail(username: String): String {
    // TODO: ensure that there is an email address for the user

    // 1. Create token for login for the user
    val token = tokenService.createToken(TokenType.MFA, username)

    // 2. Generate MFA code for user
    val code = tokenService.createToken(TokenType.MFA_CODE, username)

    // 3. Create email with both login and mfa token

    // 4. Send email
    return token
  }

  open fun validateMfaCode(code: String): Optional<String> {
    // 1. look up mfa code
    // 2. fail if expired or invalid - incorrect password attempts?
    // 3. return empty if okay
    return Optional.empty()
  }
}

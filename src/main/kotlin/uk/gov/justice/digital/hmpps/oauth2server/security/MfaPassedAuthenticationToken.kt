package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class MfaPassedAuthenticationToken(
  principal: Any?,
  credentials: Any?,
  authorities: MutableCollection<out GrantedAuthority>?
) : UsernamePasswordAuthenticationToken(principal, credentials, authorities)

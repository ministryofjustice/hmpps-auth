package uk.gov.justice.digital.hmpps.oauth2server.security

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Service("nomisUserDetailsService")
@Transactional(readOnly = true, noRollbackFor = [UsernameNotFoundException::class])
class NomisUserDetailsService(private val nomisUserService: NomisUserService) :
  UserDetailsService, AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

  @PersistenceContext(unitName = "nomis")
  private lateinit var nomisEntityManager: EntityManager

  override fun loadUserByUsername(username: String): UserDetails {
    val userPersonDetails =
      nomisUserService.getNomisUserByUsername(username).orElseThrow { UsernameNotFoundException(username) }
    // ensure that any changes to user details past this point are not persisted - e.g. by calling CredentialsContainer.eraseCredentials
    nomisEntityManager.detach(userPersonDetails)
    return userPersonDetails
  }

  override fun loadUserDetails(token: PreAuthenticatedAuthenticationToken): UserDetails = loadUserByUsername(token.name)
}

@Component
@Transactional(readOnly = true, noRollbackFor = [BadCredentialsException::class])
class NomisAuthenticationProvider(
  nomisUserDetailsService: NomisUserDetailsService,
  userRetriesService: UserRetriesService,
  mfaService: MfaService,
  userService: UserService,
  telemetryClient: TelemetryClient,
) :
  LockingAuthenticationProvider(nomisUserDetailsService, userRetriesService, mfaService, userService, telemetryClient)

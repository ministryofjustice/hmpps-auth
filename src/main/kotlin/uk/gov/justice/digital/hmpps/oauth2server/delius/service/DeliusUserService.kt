package uk.gov.justice.digital.hmpps.oauth2server.delius.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.oauth2server.config.DeliusRoleMappings
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserRole
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationServiceException
import java.util.Optional

class DeliusUserList : MutableList<UserDetails> by ArrayList()

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Service
class DeliusUserService(
  @Qualifier("deliusWebClient") private val webClient: WebClient,
  @Value("\${delius.enabled:false}") private val deliusEnabled: Boolean,
  deliusRoleMappings: DeliusRoleMappings,
) {
  private val mappings: Map<String, List<String>> =
    deliusRoleMappings.mappings.mapKeys { it.key.toUpperCase().replace('.', '_') }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getDeliusUsersByEmail(email: String): List<DeliusUserPersonDetails> {
    if (!deliusEnabled) {
      log.debug("Delius integration disabled; unable to proceed for user with email {}", email)
      return emptyList()
    }

    return try {
      val users = webClient.get().uri("/users/search/email/{email}/details", email)
        .retrieve()
        .bodyToMono(DeliusUserList::class.java)
        .block()

      users?.map { mapUserDetailsToDeliusUser(it) } ?: emptyList()
    } catch (e: Exception) {
      when (e) {
        is ResourceAccessException, is HttpServerErrorException -> {
          log.warn("Unable to retrieve details from delius for user with email {} due to delius error", email, e)
          throw DeliusAuthenticationServiceException(email)
        }
        is WebClientResponseException -> {
          log.warn(
            "Unable to retrieve details from delius for user with email {} due to http error [{}]",
            email,
            e.statusCode,
            e
          )
          emptyList()
        }
        else -> {
          log.warn("Unable to retrieve details from delius for user with email {} due to unknown error", email, e)
          emptyList()
        }
      }
    }
  }

  fun getDeliusUserByUsername(username: String): Optional<DeliusUserPersonDetails> {
    if (!deliusEnabled) {
      log.debug("Delius integration disabled, returning empty for {}", username)
      return Optional.empty()
    }

    return try {
      val userDetails = webClient.get().uri("/users/{username}/details", username)
        .retrieve()
        .bodyToMono(UserDetails::class.java)
        .block()

      Optional.ofNullable(userDetails).map { u -> mapUserDetailsToDeliusUser(u) }
    } catch (e: WebClientResponseException) {
      if (e.statusCode == HttpStatus.NOT_FOUND) {
        log.debug("User not found in delius due to {}", e.message)
      } else {
        log.warn("Unable to get delius user details for user {} due to {}", username, e.statusCode, e)
      }
      Optional.empty()
    } catch (e: Exception) {
      log.warn("Unable to retrieve details from Delius for user {} due to", username, e)
      when (e) {
        is ResourceAccessException, is HttpServerErrorException -> throw DeliusAuthenticationServiceException(username)
        else -> Optional.empty()
      }
    }
  }

  fun authenticateUser(username: String, password: String): Boolean {
    if (!deliusEnabled) {
      log.debug("Delius integration disabled, returning empty for {}", username)
      return false
    }

    return try {
      webClient.post().uri("/authenticate")
        .bodyValue(AuthUser(username, password))
        .retrieve()
        .bodyToMono(String::class.java)
        .block()

      true
    } catch (e: WebClientResponseException) {
      if (e.statusCode == HttpStatus.UNAUTHORIZED) {
        log.debug("User not authorised in delius due to {}", e.message)
      } else {
        log.warn("Unable to authenticate user {}", username, e)
      }
      false
    } catch (e: Exception) {
      log.warn("Unable to authenticate user for user {}", username, e)
      false
    }
  }

  private fun mapUserDetailsToDeliusUser(userDetails: UserDetails): DeliusUserPersonDetails =
    DeliusUserPersonDetails(
      username = userDetails.username.toUpperCase(),
      userId = userDetails.userId,
      firstName = userDetails.firstName,
      surname = userDetails.surname,
      email = userDetails.email.toLowerCase(),
      enabled = userDetails.enabled,
      roles = mapUserRolesToAuthorities(userDetails.roles)
    )

  private fun mapUserRolesToAuthorities(userRoles: List<UserRole>): Collection<GrantedAuthority> =
    userRoles.mapNotNull { (name) -> mappings[name] }
      .flatMap { r -> r.map(::SimpleGrantedAuthority) }
      .toSet()

  fun changePassword(username: String, password: String) {
    if (!deliusEnabled) {
      log.debug("Delius integration disabled, returning empty for {}", username)
      return
    }
    webClient.post().uri("/users/{username}/password", username)
      .bodyValue(AuthPassword(password))
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  data class AuthUser(val username: String, val password: String)

  data class AuthPassword(val password: String)
}

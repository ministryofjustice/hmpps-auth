package uk.gov.justice.digital.hmpps.oauth2server.azure.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.azure.AzureUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.util.*

@Service
class AzureUserService(private val userRepository: UserRepository) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getAzureUserByUsername(username: String): Optional<AzureUserPersonDetails> =
      userRepository.findByUsernameAndSource(username, AuthSource.azuread)
          .map {
            AzureUserPersonDetails(
                mutableListOf(),
                true,
                it.username,
                it.person.firstName,
                it.person.lastName,
                it.email,
                credentialsNonExpired = true,
                accountNonExpired = true,
                accountNonLocked = true)
          }
}

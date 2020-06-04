package uk.gov.justice.digital.hmpps.oauth2server.azure.service

import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.azure.AzureUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.util.*

@Slf4j
@Service
class AzureUserService(private val userRepository: UserRepository) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun getAzureUserByUsername(username: String?): Optional<AzureUserPersonDetails> {
        val user = userRepository.findByUsernameAndSource(username, AuthSource.azure)

        if (user.isPresent) {
            return Optional.of(AzureUserPersonDetails(Collections.emptyList(),
                true,
                user.get().username,
                user.get().person.firstName,
                user.get().person.lastName,
                user.get().email,
                credentialsNonExpired = true,
                password = "",
                accountNonExpired = true,
                accountNonLocked = true))
        }
        return Optional.empty()
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class PasswordGenerator(
  @Value("\${application.authentication.generated-password.length:60}") private val passwordLength: Int,
) {

  private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + ('!'..'>')
  var random: SecureRandom = SecureRandom()

  fun generatePassword(): String = (1..passwordLength)
    .map { i -> random.nextInt(92) }
    .map(charPool::get)
    .joinToString("")
}

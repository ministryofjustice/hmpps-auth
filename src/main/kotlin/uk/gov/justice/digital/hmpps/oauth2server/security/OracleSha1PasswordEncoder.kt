package uk.gov.justice.digital.hmpps.oauth2server.security

import com.google.common.primitives.Bytes
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.security.crypto.codec.Hex
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Format of oracle sha1 password is:
 * S:[password 40 characters][salt 20 characters]
 * all in uppercase hex - including salt
 */
class OracleSha1PasswordEncoder : PasswordEncoder {
  private val saltGenerator: StringKeyGenerator = Base64StringKeyGenerator()

  /**
   * Encodes the rawPass. If a salt is specified it will be merged with the password before encoding.
   *
   * @param rawPassword The plain text password
   * @return Hex string of password digest
   */
  override fun encode(rawPassword: CharSequence): String {
    val salt = saltGenerator.generateKey()
    return digest(salt, rawPassword)
  }

  private fun digest(salt: String, rawPassword: CharSequence): String {
    val saltedPassword = Bytes.concat(rawPassword.toString().toByteArray(), Hex.decode(salt))
    return "S:" + DigestUtils.sha1Hex(saltedPassword).uppercase() + salt
  }

  /**
   * Takes a previously encoded password and compares it with a rawpassword after mixing
   * in the salt and encoding that value
   *
   * @param rawPassword     plain text password
   * @param encodedPassword previously encoded password
   * @return true or false
   */
  override fun matches(rawPassword: CharSequence, encodedPassword: String?): Boolean {
    val salt = extractSalt(encodedPassword)
    val rawPasswordEncoded = digest(salt, rawPassword)
    return PasswordEncoderUtils.equals(encodedPassword, rawPasswordEncoded)
  }

  private fun extractSalt(prefixEncodedPassword: String?): String {
    return if (prefixEncodedPassword != null && prefixEncodedPassword.length >= 62) prefixEncodedPassword.substring(42, 62) else ""
  }
}

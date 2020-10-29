package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.google.common.primitives.Bytes;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Format of oracle sha1 password is:
 * S:[password 40 characters][salt 20 characters]
 * all in uppercase hex - including salt
 */
public class OracleSha1PasswordEncoder implements PasswordEncoder {
    private final StringKeyGenerator saltGenerator = new Base64StringKeyGenerator();

    /**
     * Encodes the rawPass. If a salt is specified it will be merged with the password before encoding.
     *
     * @param rawPassword The plain text password
     * @return Hex string of password digest
     */
    public String encode(final CharSequence rawPassword) {
        final var salt = saltGenerator.generateKey();
        return digest(salt, rawPassword);
    }

    private String digest(final String salt, final CharSequence rawPassword) {
        final var saltedPassword = Bytes.concat(rawPassword.toString().getBytes(), Hex.decode(salt));
        return "S:" + DigestUtils.sha1Hex(saltedPassword).toUpperCase() + salt;
    }

    /**
     * Takes a previously encoded password and compares it with a rawpassword after mixing
     * in the salt and encoding that value
     *
     * @param rawPassword     plain text password
     * @param encodedPassword previously encoded password
     * @return true or false
     */
    public boolean matches(final CharSequence rawPassword, final String encodedPassword) {
        final var salt = extractSalt(encodedPassword);
        final var rawPasswordEncoded = digest(salt, rawPassword);
        return PasswordEncoderUtils.equals(encodedPassword, rawPasswordEncoded);
    }

    private String extractSalt(final String prefixEncodedPassword) {
        return prefixEncodedPassword != null && prefixEncodedPassword.length() >= 62 ? prefixEncodedPassword.substring(42, 62) : "";
    }
}

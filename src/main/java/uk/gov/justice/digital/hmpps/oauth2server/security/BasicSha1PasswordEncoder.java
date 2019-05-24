package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BasicSha1PasswordEncoder implements PasswordEncoder {

    public String encode(final CharSequence rawPassword) {
        return digest(rawPassword);
    }

    private String digest(final CharSequence rawPassword) {
        return DigestUtils.sha1Hex(rawPassword.toString());
    }

    public boolean matches(final CharSequence rawPassword, final String encodedPassword) {
        final var rawPasswordEncoded = digest(rawPassword);
        return PasswordEncoderUtils.equals(encodedPassword, rawPasswordEncoded);
    }
}

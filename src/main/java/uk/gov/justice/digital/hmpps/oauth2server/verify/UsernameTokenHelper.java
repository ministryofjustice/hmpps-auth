package uk.gov.justice.digital.hmpps.oauth2server.verify;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@Slf4j
class UsernameTokenHelper {

    String createUsernameTokenEncodedString(final String username, final String token) {
        log.debug("Creating key for user {}", username);
        final var key = username + "-" + token;
        return Base64.encodeBase64String(key.getBytes());
    }

    Optional<UsernameToken> readUsernameTokenFromEncodedString(final String base64) {
        final var key = new String(Base64.decodeBase64(base64));
        final var index = key.indexOf('-');
        if (index == -1) {
            return Optional.empty();
        }
        final var username = key.substring(0, index);
        final var token = key.substring(index + 1);
        if (StringUtils.isBlank(username) || StringUtils.isBlank(token)) {
            return Optional.empty();
        }
        return Optional.of(new UsernameToken(username, token));
    }

    @SuppressWarnings("WeakerAccess")
    @AllArgsConstructor
    @Getter
    static class UsernameToken {
        private final String username;
        private final String token;
    }
}

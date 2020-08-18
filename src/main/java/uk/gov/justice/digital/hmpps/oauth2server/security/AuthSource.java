package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum AuthSource {
    auth, azure, delius, nomis, none;

    @JsonValue
    public String getSource() {
        return name();
    }

    public static AuthSource fromNullableString(final String source) {
        return Optional.ofNullable(source).map(s -> valueOf(source.toLowerCase())).orElse(none);
    }
}

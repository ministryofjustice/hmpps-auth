package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum AuthSource {
    nomis, auth, delius, azuread, none;

    @JsonValue
    public String getSource() {
        return name();
    }

    public static AuthSource fromNullableString(final String source) {
        return Optional.ofNullable(source).map(s -> valueOf(source.toLowerCase())).orElse(none);
    }
}

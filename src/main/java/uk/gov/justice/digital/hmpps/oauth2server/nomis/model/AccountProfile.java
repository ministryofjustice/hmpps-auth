package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import java.util.Arrays;

public enum AccountProfile {

    TAG_GENERAL,
    TAG_ADMIN;

    public static AccountProfile get(final String profile) {
        return Arrays.stream(AccountProfile.values()).filter(s -> s.name().equals(profile)).findFirst().orElse(null);
    }
}

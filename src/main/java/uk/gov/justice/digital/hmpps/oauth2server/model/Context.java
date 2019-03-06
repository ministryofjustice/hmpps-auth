package uk.gov.justice.digital.hmpps.oauth2server.model;

import java.util.Arrays;

public enum Context {
    LICENCES;

    public String getContext() {
        return this.name().toLowerCase();
    }

    public static Context get(final String context) {
        return Arrays.stream(Context.values()).filter(s -> s.name().equalsIgnoreCase(context)).findFirst().orElse(null);
    }
}

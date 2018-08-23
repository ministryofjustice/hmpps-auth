package uk.gov.justice.digital.hmpps.oauth2server.security;

import java.util.Arrays;

public enum AccountStatus {

    LOCKED("LOCKED"),
    EXPIRED_GRACE("EXPIRED(GRACE)"),
    OPEN("OPEN"),
    EXPIRED("EXPIRED"),
    EXPIRED_LOCKED("EXPIRED & LOCKED"),
    LOCKED_TIMED("LOCKED(TIMED)");

    private final String code;

    AccountStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AccountStatus get(String code) {
        return Arrays.stream(AccountStatus.values()).filter(s -> s.getCode().equals(code)).findFirst().orElse(null);
    }
}

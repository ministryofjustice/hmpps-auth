package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import java.util.Arrays;

public enum AccountStatus {

    OPEN(0,"OPEN"),                                                         // 0000
    EXPIRED(1,"EXPIRED"),                                                   // 0001
    EXPIRED_GRACE(2,"EXPIRED(GRACE)"),                                      // 0010
    LOCKED_TIMED(4,"LOCKED(TIMED)"),                                        // 0100
    LOCKED(8,"LOCKED"),                                                     // 1000
    EXPIRED_LOCKED_TIMED(5,"EXPIRED & LOCKED(TIMED)"),                      // 0101
    EXPIRED_GRACE_LOCKED_TIMED(6,"EXPIRED(GRACE) & LOCKED(TIMED)"),         // 0110
    EXPIRED_LOCKED(9,"EXPIRED & LOCKED"),                                   // 1001
    EXPIRED_GRACE_LOCKED(10,"EXPIRED(GRACE) & LOCKED");                     // 1010

    private final int code;
    private final String desc;

    AccountStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AccountStatus get(int code) {
        return Arrays.stream(AccountStatus.values()).filter(s -> s.getCode() == code).findFirst().orElse(null);
    }

    public static AccountStatus get(String desc) {
        return Arrays.stream(AccountStatus.values()).filter(s -> s.getDesc().equals(desc)).findFirst().orElse(null);
    }
}

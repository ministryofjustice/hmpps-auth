package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import java.util.Arrays;

public enum AccountStatus {

    OPEN(0,"OPEN", false, false, false),                                                         // 0000
    EXPIRED(1,"EXPIRED", true, false, false),                                                    // 0001
    EXPIRED_GRACE(2,"EXPIRED(GRACE)", true, false, true),                                        // 0010
    LOCKED_TIMED(4,"LOCKED(TIMED)", false, true, false),                                         // 0100
    LOCKED(8,"LOCKED", false, true, false),                                                      // 1000
    EXPIRED_LOCKED_TIMED(5,"EXPIRED & LOCKED(TIMED)", true, true, false),                        // 0101
    EXPIRED_GRACE_LOCKED_TIMED(6,"EXPIRED(GRACE) & LOCKED(TIMED)", true, true, true),            // 0110
    EXPIRED_LOCKED(9,"EXPIRED & LOCKED", true, true, false),                                     // 1001
    EXPIRED_GRACE_LOCKED(10,"EXPIRED(GRACE) & LOCKED", true, true, true);                        // 1010

    private final int code;
    private final String desc;
    private final boolean expired;
    private final boolean locked;
    private final boolean gracePeriod;


    AccountStatus(int code, String desc, boolean expired, boolean locked, boolean gracePeriod) {
        this.code = code;
        this.desc = desc;
        this.expired = expired;
        this.locked = locked;
        this.gracePeriod = gracePeriod;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isGracePeriod() {
        return gracePeriod;
    }

    public static AccountStatus get(int code) {
        return Arrays.stream(AccountStatus.values()).filter(s -> s.getCode() == code).findFirst().orElse(null);
    }

    public static AccountStatus get(String desc) {
        return Arrays.stream(AccountStatus.values()).filter(s -> s.getDesc().equals(desc)).findFirst().orElse(null);
    }
}

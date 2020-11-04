package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import java.util.Arrays;

public enum AccountStatus {

    OPEN(0, "OPEN", false, false, false, false),                                                    // 0000
    EXPIRED(1, "EXPIRED", true, false, false, false),                                               // 0001
    EXPIRED_GRACE(2, "EXPIRED(GRACE)", true, false, true, false),                                   // 0010
    LOCKED_TIMED(4, "LOCKED(TIMED)", false, true, false, true),                                     // 0100
    LOCKED(8, "LOCKED", false, true, false, false),                                                 // 1000
    EXPIRED_LOCKED_TIMED(5, "EXPIRED & LOCKED(TIMED)", true, true, false, true),                    // 0101
    EXPIRED_GRACE_LOCKED_TIMED(6, "EXPIRED(GRACE) & LOCKED(TIMED)", true, true, true, true),        // 0110
    EXPIRED_LOCKED(9, "EXPIRED & LOCKED", true, true, false, false),                                // 1001
    EXPIRED_GRACE_LOCKED(10, "EXPIRED(GRACE) & LOCKED", true, true, true, false);                   // 1010

    private final int code;
    private final String desc;
    private final boolean expired;
    private final boolean locked;
    private final boolean gracePeriod;
    /**
     * whether the user has locked themselves out by getting password incorrect in c-nomis
     **/
    private final boolean userLocked;


    AccountStatus(final int code, final String desc, final boolean expired, final boolean locked, final boolean gracePeriod, final boolean userLocked) {
        this.code = code;
        this.desc = desc;
        this.expired = expired;
        this.locked = locked;
        this.gracePeriod = gracePeriod;
        this.userLocked = userLocked;
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

    public boolean isUserLocked() {
        return userLocked;
    }

    public static AccountStatus get(final int code) {
        return Arrays.stream(AccountStatus.values()).filter(s -> s.getCode() == code).findFirst().orElse(null);
    }

    public static AccountStatus get(final String desc) {
        return Arrays.stream(AccountStatus.values()).filter(s -> s.getDesc().equals(desc)).findFirst().orElse(null);
    }
}

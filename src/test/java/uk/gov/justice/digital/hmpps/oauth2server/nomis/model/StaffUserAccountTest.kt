package uk.gov.justice.digital.hmpps.oauth2server.nomis.model;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class StaffUserAccountTest {

    @Test
    public void isCredentialsNonExpired_statusExpired() {
        final var account = createStaffUserAccount("EXPIRED", null);

        assertThat(account.isCredentialsNonExpired()).isFalse();
    }

    @Test
    public void isCredentialsNonExpired_statusExpiredLocked() {
        final var account = createStaffUserAccount("EXPIRED(LOCKED)", null);

        assertThat(account.isCredentialsNonExpired()).isTrue();
    }

    @Test
    public void isCredentialsNonExpired_statusExpiredTimed() {
        final var account = createStaffUserAccount("EXPIRED(TIMED)", null);

        assertThat(account.isCredentialsNonExpired()).isTrue();
    }

    @Test
    public void isCredentialsNonExpired_openStatusDateExpired() {
        final var account = createStaffUserAccount("OPEN", LocalDateTime.now().minusMinutes(1));

        assertThat(account.isCredentialsNonExpired()).isTrue();
    }

    @Test
    public void isCredentialsNonExpired_statusGraceExpired() {
        final var account = createStaffUserAccount("EXPIRED(GRACE)", null);

        assertThat(account.isCredentialsNonExpired()).isTrue();
    }

    private StaffUserAccount createStaffUserAccount(final String status, final LocalDateTime passwordExpiry) {
        final var account = new StaffUserAccount();
        final var detail = new AccountDetail();
        account.setAccountDetail(detail);

        detail.setAccountStatus(status);
        return account;
    }

}

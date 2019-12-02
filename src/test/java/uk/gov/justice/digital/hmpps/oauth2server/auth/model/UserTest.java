package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import org.junit.Test;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {
    @Test
    public void testCreateTokenOverwritesPrevious() {
        final var user = User.of("user");
        user.createToken(TokenType.RESET);
        final var changeToken = user.createToken(TokenType.CHANGE);
        final var resetToken = user.createToken(TokenType.RESET);

        assertThat(user.getTokens()).containsOnly(changeToken, resetToken);
        assertThat(user.getTokens()).extracting(UserToken::getToken).containsOnly(changeToken.getToken(), resetToken.getToken());
    }

    @Test
    public void fromUserPersonDetails() {
        final var staffUserAccountForBob = getStaffUserAccountForBob();
        final var user = User.fromUserPersonDetails(staffUserAccountForBob);
        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getSource()).isEqualTo(AuthSource.nomis);
    }

    private UserPersonDetails getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile", null);
        staffUserAccount.setAccountDetail(detail);
        staffUserAccount.setUsername("bob");
        return staffUserAccount;
    }
}

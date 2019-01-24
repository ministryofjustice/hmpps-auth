package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import org.junit.Test;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTokenTest {
    @Test
    public void testChangePasswordTokenLastsTwentyMinutes() {
        final LocalDateTime twentyMinutesTime = LocalDateTime.now().plusMinutes(20);
        final UserToken userToken = new UserToken(TokenType.CHANGE, null);
        assertThat(userToken.getTokenExpiry()).isAfterOrEqualTo(twentyMinutesTime);
    }

    @Test
    public void testChangePasswordTokenLastsNoMoreThanTwentyTwoMinutes() {
        final LocalDateTime twentyTwoMinutesTime = LocalDateTime.now().plusMinutes(22);
        final UserToken userToken = new UserToken(TokenType.CHANGE, null);
        assertThat(userToken.getTokenExpiry()).isBeforeOrEqualTo(twentyTwoMinutesTime);
    }

    @Test
    public void testResetPasswordTokenLastsOneDay() {
        final LocalDateTime oneDaysTime = LocalDateTime.now().plusDays(1);
        final UserToken userToken = new UserToken(TokenType.RESET, null);
        assertThat(userToken.getTokenExpiry()).isAfterOrEqualTo(oneDaysTime);
    }

    @Test
    public void testResetPasswordTokenLastsNoMoreThanOneDayAndTwoMinutes() {
        final LocalDateTime oneDayAndTwoMinutesTime = LocalDateTime.now().plusDays(1).plusMinutes(2);
        final UserToken userToken = new UserToken(TokenType.RESET, null);
        assertThat(userToken.getTokenExpiry()).isBeforeOrEqualTo(oneDayAndTwoMinutesTime);
    }

    @Test
    public void testVerifyTokenLastsOneDay() {
        final LocalDateTime oneDaysTime = LocalDateTime.now().plusDays(1);
        final UserToken userToken = new UserToken(TokenType.VERIFIED, null);
        assertThat(userToken.getTokenExpiry()).isAfterOrEqualTo(oneDaysTime);
    }

    @Test
    public void testVerifyTokenLastsNoMoreThanOneDayAndTwoMinutes() {
        final LocalDateTime oneDayAndTwoMinutesTime = LocalDateTime.now().plusDays(1).plusMinutes(2);
        final UserToken userToken = new UserToken(TokenType.VERIFIED, null);
        assertThat(userToken.getTokenExpiry()).isBeforeOrEqualTo(oneDayAndTwoMinutesTime);
    }
}

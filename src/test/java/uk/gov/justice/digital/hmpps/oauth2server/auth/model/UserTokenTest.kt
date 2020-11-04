package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.CHANGE
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.MFA
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.MFA_CODE
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.RESET
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType.VERIFIED
import java.time.LocalDateTime

class UserTokenTest {

  val user = buildUser()
  @Test
  fun `test change password token lasts twenty minutes`() {
    val twentyMinutesTime = LocalDateTime.now().plusMinutes(20)
    val userToken = UserToken(CHANGE, user)
    assertThat(userToken.tokenExpiry).isAfterOrEqualTo(twentyMinutesTime)
  }

  @Test
  fun `test change password token lasts no more than twenty two minutes`() {
    val twentyTwoMinutesTime = LocalDateTime.now().plusMinutes(22)
    val userToken = UserToken(CHANGE, user)
    assertThat(userToken.tokenExpiry).isBeforeOrEqualTo(twentyTwoMinutesTime)
  }

  @Test
  fun `test reset password token lasts one day`() {
    val oneDaysTime = LocalDateTime.now().plusDays(1)
    val userToken = UserToken(RESET, user)
    assertThat(userToken.tokenExpiry).isAfterOrEqualTo(oneDaysTime)
  }

  @Test
  fun `test reset password token lasts no more than one day and two minutes`() {
    val oneDayAndTwoMinutesTime = LocalDateTime.now().plusDays(1).plusMinutes(2)
    val userToken = UserToken(RESET, user)
    assertThat(userToken.tokenExpiry).isBeforeOrEqualTo(oneDayAndTwoMinutesTime)
  }

  @Test
  fun `test verify token lasts one day`() {
    val oneDaysTime = LocalDateTime.now().plusDays(1)
    val userToken = UserToken(VERIFIED, user)
    assertThat(userToken.tokenExpiry).isAfterOrEqualTo(oneDaysTime)
  }

  @Test
  fun `test verify token lasts no more than one day and two minutes`() {
    val oneDayAndTwoMinutesTime = LocalDateTime.now().plusDays(1).plusMinutes(2)
    val userToken = UserToken(VERIFIED, user)
    assertThat(userToken.tokenExpiry).isBeforeOrEqualTo(oneDayAndTwoMinutesTime)
  }

  @Test
  fun `test mfa password token lasts twenty minutes`() {
    val twentyMinutesTime = LocalDateTime.now().plusMinutes(20)
    val userToken = UserToken(MFA, user)
    assertThat(userToken.tokenExpiry).isAfterOrEqualTo(twentyMinutesTime)
  }

  @Test
  fun `test mfa password token lasts no more than twenty two minutes`() {
    val twentyTwoMinutesTime = LocalDateTime.now().plusMinutes(22)
    val userToken = UserToken(MFA, user)
    assertThat(userToken.tokenExpiry).isBeforeOrEqualTo(twentyTwoMinutesTime)
  }

  @Test
  fun `test mfa code token is correct length`() {
    val token = UserToken(MFA_CODE, user)
    assertThat(token.token).hasSize(6).containsOnlyDigits()
  }

  @Test
  fun `test other tokens are correct length`() {
    val token = UserToken(MFA, user)
    assertThat(token.token).hasSize(36)
  }
}

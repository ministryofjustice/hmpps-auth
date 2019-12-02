package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.time.LocalDateTime

class StaffUserAccountTest {
  @Test
  fun `isCredentialsNonExpired status expired`() {
    val account = createStaffUserAccount("EXPIRED")
    assertThat(account.isCredentialsNonExpired).isFalse()
  }

  @Test
  fun `isCredentialsNonExpired status expired Locked`() {
    val account = createStaffUserAccount("EXPIRED(LOCKED)")
    assertThat(account.isCredentialsNonExpired).isTrue()
  }

  @Test
  fun `isCredentialsNonExpired status expired Timed`() {
    val account = createStaffUserAccount("EXPIRED(TIMED)")
    assertThat(account.isCredentialsNonExpired).isTrue()
  }

  @Test
  fun `isCredentialsNonExpired open status date expired`() {
    val account = createStaffUserAccount("OPEN", LocalDateTime.now().minusMinutes(1))
    assertThat(account.isCredentialsNonExpired).isTrue()
  }

  @Test
  fun `isCredentialsNonExpired status grace expired`() {
    val account = createStaffUserAccount("EXPIRED(GRACE)")
    assertThat(account.isCredentialsNonExpired).isTrue()
  }

  @Test
  fun `to user copy username`() {
    val user = createStaffUserAccount().toUser()
    assertThat(user.username).isEqualTo("user")
  }

  @Test
  fun `to user unverified email address`() {
    val user = createStaffUserAccount().toUser()
    assertThat(user.isVerified).isEqualTo(false)
  }

  @Test
  fun `to user nomis source`() {
    val user = createStaffUserAccount().toUser()
    assertThat(user.source).isEqualTo(AuthSource.nomis)
  }

  private fun createStaffUserAccount(status: String = "STATUS", passwordExpiry: LocalDateTime? = null): StaffUserAccount {
    val account = StaffUserAccount()
    val detail = AccountDetail()
    account.accountDetail = detail
    account.username = "user"
    detail.accountStatus = status
    return account
  }
}

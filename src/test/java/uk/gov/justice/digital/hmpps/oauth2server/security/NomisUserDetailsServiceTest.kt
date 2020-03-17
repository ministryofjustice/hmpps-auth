package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.EXPIRED
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.EXPIRED_GRACE
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.EXPIRED_LOCKED
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.LOCKED
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.LOCKED_TIMED
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountStatus.OPEN
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Role
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.UserCaseloadRole
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.UserCaseloadRoleIdentity
import java.util.*
import javax.persistence.EntityManager

class NomisUserDetailsServiceTest {
  private val userService: NomisUserService = mock()
  private val nomisEntityManager: EntityManager = mock()
  private val service = NomisUserDetailsService(userService)

  @BeforeEach
  fun setup() {
    ReflectionTestUtils.setField(service, "nomisEntityManager", nomisEntityManager)
  }

  @Test
  fun testHappyUserPath() {
    val user = buildStandardUser("ITAG_USER")
    whenever(userService.getNomisUserByUsername(user.username)).thenReturn(Optional.of(user))
    val itagUser = service.loadUserByUsername(user.username)
    assertThat(itagUser).isNotNull()
    assertThat(itagUser.isAccountNonExpired).isTrue()
    assertThat(itagUser.isAccountNonLocked).isTrue()
    assertThat(itagUser.isCredentialsNonExpired).isTrue()
    assertThat(itagUser.isEnabled).isTrue()
    assertThat((itagUser as UserPersonDetails).name).isEqualTo("Itag User")
  }

  @Test
  fun testEntityDetached() {
    val user = buildStandardUser("ITAG_USER")
    whenever(userService.getNomisUserByUsername(user.username)).thenReturn(Optional.of(user))
    val itagUser = service.loadUserByUsername(user.username)
    Mockito.verify(nomisEntityManager).detach(user)
    assertThat((itagUser as UserPersonDetails).name).isEqualTo("Itag User")
  }

  @Test
  fun testLockedUser() {
    val user = buildLockedUser()
    whenever(userService.getNomisUserByUsername(user.username)).thenReturn(Optional.of(user))
    val itagUser = service.loadUserByUsername(user.username)
    assertThat(itagUser).isNotNull()
    assertThat(itagUser.isAccountNonExpired).isTrue()
    assertThat(itagUser.isAccountNonLocked).isFalse()
    assertThat(itagUser.isCredentialsNonExpired).isTrue()
    assertThat(itagUser.isEnabled).isFalse()
  }

  @Test
  fun testExpiredUser() {
    val user = buildExpiredUser()
    whenever(userService.getNomisUserByUsername(user.username)).thenReturn(Optional.of(user))
    val itagUser = service.loadUserByUsername(user.username)
    assertThat(itagUser).isNotNull()
    assertThat(itagUser.isAccountNonExpired).isTrue()
    assertThat(itagUser.isAccountNonLocked).isTrue()
    assertThat(itagUser.isCredentialsNonExpired).isFalse()
    assertThat(itagUser.isEnabled).isTrue()
  }

  @Test
  fun testUserNotFound() {
    whenever(userService.getNomisUserByUsername(anyString())).thenReturn(Optional.empty())
    assertThatThrownBy { service.loadUserByUsername("user") }.isInstanceOf(UsernameNotFoundException::class.java)
  }

  @Test
  fun testExpiredGraceUser() {
    val user = buildExpiredGraceUser()
    whenever(userService.getNomisUserByUsername(user.username)).thenReturn(Optional.of(user))
    val itagUser = service.loadUserByUsername(user.username)
    assertThat(itagUser).isNotNull()
    assertThat(itagUser.isAccountNonExpired).isTrue()
    assertThat(itagUser.isAccountNonLocked).isTrue()
    assertThat(itagUser.isCredentialsNonExpired).isTrue()
    assertThat(itagUser.isEnabled).isTrue()
  }

  @Test
  fun testExpiredLockedUser() {
    val user = buildExpiredLockedUser()
    whenever(userService.getNomisUserByUsername(user.username)).thenReturn(Optional.of(user))
    val itagUser = service.loadUserByUsername(user.username)
    assertThat(itagUser).isNotNull()
    assertThat(itagUser.isAccountNonLocked).isFalse()
    assertThat(itagUser.isCredentialsNonExpired).isFalse()
    assertThat(itagUser.isEnabled).isFalse()
  }

  @Test
  fun testLockedTimedUser() {
    val user = buildLockedTimedUser()
    whenever(userService.getNomisUserByUsername(user.username)).thenReturn(Optional.of(user))
    val itagUser = service.loadUserByUsername(user.username)
    assertThat(itagUser).isNotNull()
    assertThat(itagUser.isEnabled).isFalse()
    assertThat(itagUser.isAccountNonExpired).isTrue()
    assertThat(itagUser.isAccountNonLocked).isFalse()
    assertThat(itagUser.isCredentialsNonExpired).isTrue()
  }

  private fun buildStandardUser(username: String): NomisUserPersonDetails {
    val staff = buildStaff()
    return NomisUserPersonDetails.builder()
        .username(username)
        .password("pass")
        .type("GENERAL")
        .staff(staff)
        .roles(listOf(UserCaseloadRole.builder()
            .id(UserCaseloadRoleIdentity.builder().caseload("NWEB").roleId(ROLE_ID).username(username).build())
            .role(Role.builder().code("ROLE1").id(ROLE_ID).build())
            .build()))
        .accountDetail(buildAccountDetail(username, OPEN))
        .build()
  }

  private fun buildExpiredUser(): NomisUserPersonDetails {
    val userAccount = buildStandardUser("EXPIRED_USER")
    userAccount.accountDetail = buildAccountDetail("EXPIRED_USER", EXPIRED)
    return userAccount
  }

  private fun buildLockedUser(): NomisUserPersonDetails {
    val userAccount = buildStandardUser("LOCKED_USER")
    userAccount.accountDetail = buildAccountDetail("LOCKED_USER", LOCKED)
    return userAccount
  }

  private fun buildExpiredLockedUser(): NomisUserPersonDetails {
    val userAccount = buildStandardUser("EXPIRED_USER")
    userAccount.accountDetail = buildAccountDetail("EXPIRED_USER", EXPIRED_LOCKED)
    return userAccount
  }

  private fun buildLockedTimedUser(): NomisUserPersonDetails {
    val userAccount = buildStandardUser("LOCKED_USER")
    userAccount.accountDetail = buildAccountDetail("LOCKED_USER", LOCKED_TIMED)
    return userAccount
  }

  private fun buildExpiredGraceUser(): NomisUserPersonDetails {
    val userAccount = buildStandardUser("EXPIRED_USER")
    userAccount.accountDetail = buildAccountDetail("EXPIRED_USER", EXPIRED_GRACE)
    return userAccount
  }

  private fun buildAccountDetail(username: String, status: AccountStatus): AccountDetail {
    return AccountDetail.builder()
        .username(username)
        .accountStatus(status.desc)
        .profile("TAG_GENERAL")
        .build()
  }

  private fun buildStaff(): Staff {
    return Staff.builder()
        .staffId(1L)
        .firstName("ITAG")
        .lastName("USER")
        .status("ACTIVE")
        .build()
  }

  companion object {
    private const val ROLE_ID = 1L
  }
}

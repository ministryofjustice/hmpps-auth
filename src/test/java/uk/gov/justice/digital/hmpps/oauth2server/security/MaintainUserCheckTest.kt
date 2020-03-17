package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import java.util.*

class MaintainUserCheckTest {
  private val userRepository: UserRepository = mock()
  private val maintainUserCheck = MaintainUserCheck(userRepository)

  @Test
  fun superUserDoesNotThrowError() {
    val user = User.of("user")
    assertThatCode { maintainUserCheck.ensureUserLoggedInUserRelationship("SuperUser", SUPER_USER, user) }.doesNotThrowAnyException()
  }

  @Test
  fun groupManagerGroupInCommonWithUserDoesNotThrowError() {
    val user = User.of("user")
    val group1 = Group("group", "desc")
    user.groups = setOf(group1, Group("group2", "desc"))
    val groupManager = User.of("groupManager")
    groupManager.groups = setOf(Group("group3", "desc"), group1)
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString()))
        .thenReturn(Optional.of(groupManager))
    assertThatCode { maintainUserCheck.ensureUserLoggedInUserRelationship("GroupManager", GROUP_MANAGER, user) }.doesNotThrowAnyException()
    verify(userRepository).findByUsernameAndMasterIsTrue(anyString())
  }

  @Test
  fun groupManagerNoGroupInCommonWithUserThrowsError() {
    val user = User.of("user")
    val optionalUserEmail = createUser()
    whenever(userRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(optionalUserEmail)
    assertThatThrownBy { maintainUserCheck.ensureUserLoggedInUserRelationship("GroupManager", GROUP_MANAGER, user) }.isInstanceOf(AuthUserGroupRelationshipException::class.java).hasMessage("Unable to maintain user: user with reason: User not with your groups")
    verify(userRepository).findByUsernameAndMasterIsTrue(anyString())
  }

  private fun createUser() = Optional.of(User.of("someUser"))

  companion object {
    private val SUPER_USER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_MAINTAIN_OAUTH_USERS"))
    private val GROUP_MANAGER: Set<GrantedAuthority> = setOf(SimpleGrantedAuthority("ROLE_AUTH_GROUP_MANAGER"))
  }
}

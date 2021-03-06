package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup

class AuthAllGroupsControllerTest {
  private val authUserGroupService: AuthUserGroupService = mock()
  private val controller = AuthAllGroupsController(authUserGroupService)

  @Test
  fun allGroups() {
    val group1 = Group("FRED", "desc")
    val group2 = Group("GLOBAL_SEARCH", "desc2")
    val groups = listOf(group2, group1)
    whenever(authUserGroupService.allGroups).thenReturn(groups)
    val response = controller.allGroups()
    assertThat(response).containsOnly(AuthUserGroup(group1), AuthUserGroup(group2))
  }
}

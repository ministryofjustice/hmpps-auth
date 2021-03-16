package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest

class AuthUserGroupsControllerIntTest : IntegrationTest() {

  private val invalidToken =
    "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiJlZGYzOWQwMy03YmJkLTQ2ZGYtOTQ5Ny1mYzI2MDg2ZWIzYTgiLCJzdWIiOiJJVEFHX1VTRVJfQURNIiwidXNlcl9uYW1lIjoiSVRBR19VU0VSX0FETSIsImNsaWVudF9pZCI6ImVsaXRlMmFwaWNsaWVudCIsImF1dGhvcml0aWVzIjpbXSwic2NvcGUiOlsicmVhZCIsIndyaXRlIl0sImV4cCI6MTYwMzM2NjY0N30.Vi4z77ylpS94ztVyEQoilkRuMDDDfvYVPblQRmUA5ACo3TF4-9NW2xE1Hm4hURwesayMs_apBrW2iAbPVtiTRC_TiMFApPXU-SoMadO5QcqKumXx_z2HfV_J_1eQKS0RJBxaz89xdeR2ilTTEmUyk38IulFJ0IVY2k65gCkQffKn6uE3K4NDBATQXbBwQZ7Soqr89fmsh-xym9JCA63AB_aU42S39sWl7OtUildrf9UgNv81rnOSs1eLDFdcmgztUSdac2hyX01u0vai51biz93-IBF5xdIdAInDmNktF9jwrYsindDu3LCiubrqGuK3MScZDB7A_OW5gHSfyCHmvw"

  @Test
  fun `Auth User Groups add group endpoint adds a group to a user`() {

    callGetGroups()
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_2')]")
      .doesNotExist()

    webTestClient
      .put().uri("/api/authuser/AUTH_RO_USER/groups/site_1_group_2")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isNoContent

    callGetGroups()
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_2')]")
      .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_2", "groupName" to "Site 1 - Group 2"))
  }

  @Test
  fun `Auth User Groups remove group endpoint removes a group from a user`() {
    callGetGroups()
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_1", "groupName" to "Site 1 - Group 1"))

    webTestClient
      .delete().uri("/api/authuser/AUTH_RO_USER/groups/site_1_group_1")
      .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isNoContent

    callGetGroups()
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .doesNotExist()
  }

  @Test
  fun `Auth User Groups add group endpoint adds a group to a user - group manager`() {

    callGetGroups(user = "AUTH_RO_USER_TEST3")
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_2')]")
      .doesNotExist()

    webTestClient
      .put().uri("/api/authuser/AUTH_RO_USER_TEST3/groups/SITE_1_GROUP_2")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isNoContent

    callGetGroups(user = "AUTH_RO_USER_TEST3")
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_2')]")
      .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_2", "groupName" to "Site 1 - Group 2"))
  }

  @Test
  fun `Auth User Groups remove group endpoint removes a group from a user - group manager`() {
    callGetGroups(user = "AUTH_RO_USER_TEST3")
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_1", "groupName" to "Site 1 - Group 1"))

    webTestClient
      .delete().uri("/api/authuser/AUTH_RO_USER_TEST3/groups/SITE_1_GROUP_1")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isNoContent

    callGetGroups(user = "AUTH_RO_USER_TEST3")
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .doesNotExist()
  }

  @Test
  fun `Auth User Groups add group endpoint does not add group if group Manager not member of group`() {

    callGetGroups(user = "AUTH_RO_USER_TEST3")
      .jsonPath(".[?(@.groupCode == 'PECS_DRB8')]")
      .doesNotExist()

    webTestClient
      .put().uri("/api/authuser/AUTH_RO_USER_TEST3/groups/PECS_DRB8")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isBadRequest

    callGetGroups(user = "AUTH_RO_USER_TEST3")
      .jsonPath(".[?(@.groupCode == 'PECS_DRB8')]")
      .doesNotExist()
  }

  @Test
  fun `Auth User Groups add group endpoint does not add group if user not in group managers groups`() {

    callGetGroups(user = "AUTH_RO_USER_TEST4")
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .doesNotExist()

    webTestClient
      .put().uri("/api/authuser/AUTH_RO_USER_TEST4/groups/SITE_1_GROUP_1")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isForbidden

    callGetGroups(user = "AUTH_RO_USER_TEST4")
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .doesNotExist()
  }

  @Test
  fun `Auth User Groups remove group endpoint does not remove group if group Manager not member of group`() {
    callGetGroups(user = "AUTH_RO_USER_TEST3")
      .jsonPath(".[?(@.groupCode == 'GC_DEL_4')]")
      .isEqualTo(mapOf("groupCode" to "GC_DEL_4", "groupName" to "Group 4 for deleting"))

    webTestClient
      .delete().uri("/api/authuser/AUTH_RO_USER_TEST3/groups/GC_DEL_4")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isBadRequest

    callGetGroups(user = "AUTH_RO_USER_TEST3")
      .jsonPath(".[?(@.groupCode == 'GC_DEL_4')]")
      .isEqualTo(mapOf("groupCode" to "GC_DEL_4", "groupName" to "Group 4 for deleting"))
  }

  @Test
  fun `Auth User Groups remove group endpoint does not remove group if group Manager and users last group`() {
    callGetGroups(user = "AUTH_RO_USER_TEST5")
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_1", "groupName" to "Site 1 - Group 1"))

    webTestClient
      .delete().uri("/api/authuser/AUTH_RO_USER_TEST5/groups/SITE_1_GROUP_1")
      .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
      .exchange()
      .expectStatus().isForbidden

    callGetGroups(user = "AUTH_RO_USER_TEST5")
      .jsonPath(".[?(@.groupCode == 'SITE_1_GROUP_1')]")
      .isEqualTo(mapOf("groupCode" to "SITE_1_GROUP_1", "groupName" to "Site 1 - Group 1"))
  }

  @Test
  fun `Auth User Groups endpoint returns user groups no children`() {
    webTestClient
      .get().uri("/api/authuser/auth_ro_vary_user/groups?children=false")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(
        """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"SITE_1_GROUP_2","groupName":"Site 1 - Group 2"}       
        ]
        """.trimIndent()
      )
  }

  @Test
  fun `Auth User Groups endpoint returns user groups with children by default`() {
    webTestClient
      .get().uri("/api/authuser/auth_ro_vary_user/groups")
      .headers(setAuthorisation("ITAG_USER_ADM"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(
        """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"CHILD_1","groupName":"Child - Site 1 - Group 2"}
        ]
        """.trimIndent()
      )
  }

  @Test
  fun `Auth User Groups endpoint returns user groups with children`() {
    callGetGroups("auth_ro_vary_user", children = true)
      .json(
        """[
          {"groupCode":"SITE_1_GROUP_1","groupName":"Site 1 - Group 1"},
          {"groupCode":"CHILD_1","groupName":"Child - Site 1 - Group 2"}
        ]
        """.trimIndent()
      )
  }

  @Test
  fun `Auth User Groups endpoint not accessible without valid token`() {
    webTestClient
      .get().uri("/api/authuser/auth_ro_vary_user/groups")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `Auth User Groups add group endpoint not accessible without valid token`() {
    webTestClient
      .put().uri("/api/authuser/auth_ro_vary_user/groups/licence_ro")
      .header("Authorization", "Basic $invalidToken")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `Auth User Groups remove group endpoint not accessible without valid token`() {
    webTestClient
      .delete().uri("/api/authuser/auth_ro_vary_user/groups/licence_ro")
      .header("Authorization", "Basic $invalidToken")
      .exchange()
      .expectStatus().isUnauthorized
  }

  private fun callGetGroups(user: String = "AUTH_RO_USER", children: Boolean = false): BodyContentSpec = webTestClient
    .get().uri("/api/authuser/$user/groups?children=$children")
    .headers(setAuthorisation("ITAG_USER_ADM"))
    .exchange()
    .expectStatus().isOk
    .expectBody()
}

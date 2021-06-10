package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ServiceTest {

  @Nested
  inner class GetRoles {
    @Test
    fun roles() {
      val service =
        Service("CODE", "NAME", "Description", "SOME_ROLE, , ,  SOME_OTHER_ROLE", "http://some.url", true, "a@b.com")
      assertThat(service.roles).containsExactly("SOME_ROLE", "SOME_OTHER_ROLE")
    }

    @Test
    fun `authorisation roles is null`() {
      val service = Service("CODE", "NAME", "Description", null, "http://some.url", true, "a@b.com")
      assertThat(service.roles).isEmpty()
    }
    @Test
    fun `authorisation roles is an empty string`() {
      val service = Service("CODE", "NAME", "Description", "", "http://some.url", true, "a@b.com")
      assertThat(service.roles).isEmpty()
    }
  }

  @Nested
  inner class IsUrlInsteadOfEmail {
    @Test
    fun isUrlInsteadOfEmail() {
      val service = Service("CODE", "NAME", "Description", null, "http://some.url", true, "a@b.com")
      assertThat(service.isUrlInsteadOfEmail).isFalse()
    }

    @Test
    fun `isUrlInsteadOfEmail true`() {
      val service = Service("CODE", "NAME", "Description", null, "http://some.url", true, "http://some.url")
      assertThat(service.isUrlInsteadOfEmail).isTrue()
    }
  }

  @Nested
  inner class AuthorisedRolesWithNewlines {
    @Test
    fun authorisedRolesWithNewlines() {
      val service = Service(code = "code", name = "", description = "", url = "", authorisedRoles = "joe,bloggs,")
      assertThat(service.authorisedRolesWithNewlines).isEqualTo("joe\nbloggs\n")
    }

    @Test
    fun `authorisedRolesWithNewlines no roles`() {
      val service = Service(code = "code", name = "", description = "", url = "")
      assertThat(service.authorisedRolesWithNewlines).isEqualTo("")
    }
  }

  @Nested
  inner class SetAuthorisedRolesWithNewlines {

    @Test
    fun authorisedRolesWithNewlines() {
      val service = Service(code = "code", name = "", description = "", url = "")
      service.authorisedRolesWithNewlines = "ROLE_PRISON_ADMIN \n \n ROLE_PRISON_ORG"
      assertThat(service.authorisedRolesWithNewlines).isEqualTo("ROLE_PRISON_ADMIN\nROLE_PRISON_ORG")
    }

    @Test
    fun `authorisedRolesWithNewlines no roles`() {
      val service = Service(code = "code", name = "", description = "", url = "")
      service.authorisedRolesWithNewlines = " \n \n "
      assertThat(service.authorisedRolesWithNewlines).isEqualTo("")
    }

    @Test
    fun `authorisedRolesWithNewlines add ROLE_ prefix`() {
      val service = Service(code = "code", name = "", description = "", url = "")
      service.authorisedRolesWithNewlines = "A \n joe \n role_harry"
      assertThat(service.roles).containsExactlyInAnyOrder("ROLE_A", "ROLE_JOE", "ROLE_HARRY")
    }
  }
}

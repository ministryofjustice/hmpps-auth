package uk.gov.justice.digital.hmpps.oauth2server.delius.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UserDetailsTest {

  @Test
  fun `test deserialize`() {
    val stream = UserDetailsTest::class.java.getResourceAsStream("user_details.json")
    val userDetails = ObjectMapper().readValue(stream, UserDetails::class.java)
    assertThat(userDetails).isEqualTo(
        UserDetails(surname = "Smith",
            firstName = "John",
            email = "test@digital.justice.gov.uk",
            locked = false,
            roles = listOf(UserRole(name = "TEST_ROLE", description = "A Test Role"))))
  }

  @Test
  fun `test deserialize no roles`() {
    val stream = UserDetailsTest::class.java.getResourceAsStream("user_details_no_roles.json")
    val userDetails = ObjectMapper().readValue(stream, UserDetails::class.java)
    assertThat(userDetails).isEqualTo(
        UserDetails(surname = "Smith",
            firstName = "John",
            email = "test@digital.justice.gov.uk",
            locked = false,
            roles = listOf()))
  }
}

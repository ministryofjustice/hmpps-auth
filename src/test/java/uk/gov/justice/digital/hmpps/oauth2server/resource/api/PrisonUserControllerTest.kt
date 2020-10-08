package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService

class PrisonUserControllerTest {
  private val userService: UserService = mock()
  private val controller = PrisonUserController(userService)

  @Test
  fun `no matches`() {
    whenever(userService.findPrisonUsersByFirstAndLastNames(anyString(), anyString())).thenReturn(listOf())
    assertThat(controller.prisonUsersByFirstAndLastName("first", "last")).isEmpty()
  }

  @Test
  fun `User mapped to PrisonUser`() {
    val user = User
      .builder()
      .verified(true)
      .username("username")
      .email("user@justice.gov.uk")
      .person(Person("first", "last"))
      .build()

    whenever(userService.findPrisonUsersByFirstAndLastNames(anyString(), anyString())).thenReturn(listOf(user))

    assertThat(controller.prisonUsersByFirstAndLastName("first", "last"))
      .containsExactly(
        PrisonUser(
          username = "username",
          verified = true,
          emailAddress = "user@justice.gov.uk",
        )
      )
  }

  @Test
  fun `User mapped to PrisonUser handlign missing values`() {
    val user = User
      .builder()
      .verified(false)
      .username("username")
      .build()

    whenever(userService.findPrisonUsersByFirstAndLastNames(anyString(), anyString())).thenReturn(listOf(user))

    assertThat(controller.prisonUsersByFirstAndLastName("first", "last"))
      .containsExactly(
        PrisonUser(
          username = "username",
          verified = false,
          emailAddress = null
        )
      )
  }
}

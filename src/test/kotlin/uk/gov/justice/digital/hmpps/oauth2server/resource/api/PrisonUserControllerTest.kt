@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.PrisonUserDto
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService

class PrisonUserControllerTest {
  private val userService: UserService = mock()
  private val nomisUserService: NomisUserService = mock()
  private val controller = PrisonUserController(userService, nomisUserService, true)

  @Nested
  inner class prisonUsersByFirstAndLastName {
    @Test
    fun `no matches`() {
      whenever(userService.findPrisonUsersByFirstAndLastNames(anyString(), anyString())).thenReturn(listOf())
      assertThat(controller.prisonUsersByFirstAndLastName("first", "last")).isEmpty()
    }

    @Test
    fun `User mapped to PrisonUser`() {
      val user = PrisonUserDto(
        verified = true,
        username = "username",
        email = "user@justice.gov.uk",
        firstName = "first",
        lastName = "last",
        userId = "123456789",
        activeCaseLoadId = "MDI"
      )

      whenever(userService.findPrisonUsersByFirstAndLastNames(anyString(), anyString())).thenReturn(listOf(user))

      assertThat(controller.prisonUsersByFirstAndLastName("first", "last"))
        .containsExactly(
          PrisonUser(
            username = "username",
            staffId = 123456789,
            verified = true,
            email = "user@justice.gov.uk",
            firstName = "First",
            lastName = "Last",
            name = "First Last",
            activeCaseLoadId = "MDI"
          )
        )
    }

    @Test
    fun `User mapped to PrisonUser handling missing values`() {
      val user = PrisonUserDto(
        verified = false,
        username = "username",
        firstName = "first",
        lastName = "last",
        userId = "123456789",
        email = null,
        activeCaseLoadId = null
      )

      whenever(userService.findPrisonUsersByFirstAndLastNames(anyString(), anyString())).thenReturn(listOf(user))

      assertThat(controller.prisonUsersByFirstAndLastName("first", "last"))
        .containsExactly(
          PrisonUser(
            username = "username",
            staffId = 123456789,
            verified = false,
            firstName = "First",
            lastName = "Last",
            name = "First Last",
            email = null,
            activeCaseLoadId = null
          )
        )
    }
  }
}

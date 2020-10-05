package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl

internal class UserContextServiceTest {
  private val deliusUserService: DeliusUserService = mock()
  private val authUserService: AuthUserService = mock()
  private val userContextService = UserContextService(deliusUserService, authUserService)

  @Test
  fun `resolveUser returns the same user for clients with 'normal' scopes`() {
    val loginUser = User.builder().username("username").source(auth).build()

    var scopes = setOf("read")
    var user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isEqualTo(loginUser)

    scopes = setOf("read", "write")
    user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isEqualTo(loginUser)
  }

  @Test
  fun `resolveUser returns the same user when missing mapping implementation`() {
    val loginUser = User.builder().username("username").source(auth).build()
    val scopes = setOf("read", "delius")

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isEqualTo(loginUser)
  }

  @Test
  fun `resolveUser returns the same user when attempting to map users to the same auth source`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "delius", "userId", "jwtId")
    val scopes = setOf("delius")

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isEqualTo(loginUser)
  }

  @Test
  fun `resolveUser can map from azureAD to delius`() {
    val deliusUser = DeliusUserPersonDetails("username", "id", "user", "name", "email@email.com")
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val scopes = setOf("delius")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isEqualTo(deliusUser)
  }

  @Test
  fun `resolveUser returns the same user when the user service returns no users`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val scopes = setOf("delius")
    whenever(deliusUserService.getDeliusUsersByEmail("email@email.com")).thenReturn(emptyList())

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isEqualTo(loginUser)
  }
}

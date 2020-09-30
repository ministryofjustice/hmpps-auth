package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl

internal class UserContextServiceTest {
  private val deliusUserService: DeliusUserService = mock()
  private val userContextService = UserContextService(deliusUserService)

  @Test
  fun `resolveUser returns the same user for clients with 'normal' scopes`() {
    val loginUser = User.builder().username("username").source(auth).build()

    var scopes = setOf("read")
    var user = userContextService.resolveUser(loginUser, scopes)
    assertEquals(user, loginUser)

    scopes = setOf("read", "write")
    user = userContextService.resolveUser(loginUser, scopes)
    assertEquals(user, loginUser)
  }

  @Test
  fun `resolveUser attempts to map user for 'delius' scope but fails due to missing mapping implementation`() {
    val loginUser = User.builder().username("username").source(auth).build()
    val scopes = setOf("read", "delius")

    assertThrows(UserMappingException::class.java) {
      userContextService.resolveUser(loginUser, scopes)
    }
  }

  @Test
  fun `resolveUser returns the same user when attempting to map users to the same auth source`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "delius", "userId", "jwtId")
    val scopes = setOf("delius")

    val user = userContextService.resolveUser(loginUser, scopes)
    assertEquals(user, loginUser)
  }

  @Test
  fun `resolveUser can map from azureAD to delius`() {
    val deliusUser = DeliusUserPersonDetails("username", "id", "user", "name", "email@email.com")
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val scopes = setOf("delius")
    whenever(deliusUserService.getDeliusUserByEmail("email@email.com")).thenReturn(deliusUser)

    val user = userContextService.resolveUser(loginUser, scopes)
    assertEquals(user, deliusUser)
  }

  @Test
  fun `resolveUser returns the same user when the user service returns a null value`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val scopes = setOf("delius")
    whenever(deliusUserService.getDeliusUserByEmail("email@email.com")).thenReturn(null)

    val user = userContextService.resolveUser(loginUser, scopes)
    assertEquals(user, loginUser)
  }
}

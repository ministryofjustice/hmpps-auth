package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.auth
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import java.util.UUID

internal class UserContextServiceTest {
  private val deliusUserService: DeliusUserService = mock()
  private val authUserService: AuthUserService = mock()
  private val nomisUserService: NomisUserService = mock()
  private val userContextService = UserContextService(deliusUserService, authUserService, nomisUserService)

  @Test
  fun `discoverUsers returns empty list for clients with 'normal' scopes`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")

    val users = userContextService.discoverUsers(loginUser, setOf("read"))
    assertThat(users).isEmpty()
  }

  @Test
  fun `discoverUsers returns empty list when not azuread from mapping`() {
    val loginUser = User.builder().username("username").source(auth).id(UUID.randomUUID()).build()
    val scopes = setOf("read", "delius")

    val users = userContextService.discoverUsers(loginUser, scopes)
    assertThat(users).isEmpty()
  }

  @Test
  fun `discoverUsers can map from azureAD to delius`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "emailid@email.com", "jwtId")
    val deliusUser = DeliusUserPersonDetails("username", "id", "user", "name", "email@email.com", true)
    val scopes = setOf("delius")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))

    val users = userContextService.discoverUsers(loginUser, scopes)
    assertThat(users).containsExactly(deliusUser)

    verify(deliusUserService).getDeliusUsersByEmail("emailid@email.com")
  }

  @Test
  fun `discoverUsers tries all three sources when no valid scopes found`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "emailid@email.com", "jwtId")
    val scopes = setOf("read,write")

    userContextService.discoverUsers(loginUser, scopes)
    verify(deliusUserService).getDeliusUsersByEmail("emailid@email.com")
    verify(nomisUserService).getNomisUsersByEmail("emailid@email.com")
    verify(authUserService).findAuthUsersByEmail("emailid@email.com")
  }

  @Test
  fun `discoverUsers can map from azureAD to nomis`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "emailid@email.com", "jwtId")
    val nomisUser = NomisUserPersonDetails(
      "username",
      "",
      null,
      "GEN",
      "MDI",
      listOf(),
      AccountDetail("username", "OPEN", "GEN", null)
    )
    val scopes = setOf("nomis")
    whenever(nomisUserService.getNomisUsersByEmail(anyString())).thenReturn(listOf(nomisUser))

    val user = userContextService.discoverUsers(loginUser, scopes)
    assertThat(user).containsExactly(nomisUser)

    verify(nomisUserService).getNomisUsersByEmail("emailid@email.com")
  }

  @Test
  fun `discoverUsers returns no users when the user service returns no users`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val scopes = setOf("delius")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(emptyList())

    val users = userContextService.discoverUsers(loginUser, scopes)
    assertThat(users).isEmpty()
  }

  @Test
  fun `discoverUsers returns the empty when no to mapping exists`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val scopes = setOf("nomis")

    val users = userContextService.discoverUsers(loginUser, scopes)
    assertThat(users).isEmpty()
  }

  @Test
  fun `discoverUsers returns all users when multiple users matched`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val deliusUser = DeliusUserPersonDetails("username", "id", "user", "name", "email@email.com", true)
    val authUser = User.builder().username("username").source(auth).enabled(true).verified(true).build()
    val scopes = setOf("delius", "auth")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser))

    val users = userContextService.discoverUsers(loginUser, scopes)
    assertThat(users).containsExactlyInAnyOrder(deliusUser, authUser)
  }

  @Test
  fun `discoverUsers returns all users when multiple users matched from same source`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val authUser = User.builder().username("username").source(auth).enabled(true).verified(true).build()
    val scopes = setOf("delius", "auth")
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser, authUser))

    val users = userContextService.discoverUsers(loginUser, scopes)
    assertThat(users).hasSize(2).containsExactlyInAnyOrder(authUser, authUser)
  }

  @Test
  fun `discoverUsers ignores disabled users`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val deliusUser = DeliusUserPersonDetails(
      "username",
      "id",
      "user",
      "name",
      "email@email.com",
      enabled = false
    )
    val authUser = User.builder().username("username").source(auth).enabled(true).verified(true).build()
    val scopes = setOf("delius", "auth")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser))

    val users = userContextService.discoverUsers(loginUser, scopes)
    assertThat(users).containsExactly(authUser)
  }

  @Test
  fun `discoverUsers ignores unverified users`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val authUser = User.builder().username("username").source(auth).enabled(true).verified(true).build()
    val unverifiedAuthUser = User.builder().username("username1").source(auth).enabled(true).build()
    val scopes = setOf("auth")
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser, unverifiedAuthUser))

    val users = userContextService.discoverUsers(loginUser, scopes)
    assertThat(users).containsExactly(authUser)
  }
}

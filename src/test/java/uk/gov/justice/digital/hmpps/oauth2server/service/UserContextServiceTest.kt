package uk.gov.justice.digital.hmpps.oauth2server.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

internal class UserContextServiceTest {
  private val deliusUserService: DeliusUserService = mock()
  private val authUserService: AuthUserService = mock()
  private val nomisUserService: NomisUserService = mock()
  private val userContextService = UserContextService(deliusUserService, authUserService, nomisUserService)

  @Test
  fun `resolveUser returns the same user for clients with 'normal' scopes`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")

    var scopes = setOf("read")
    var user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(loginUser)

    scopes = setOf("read", "write")
    user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(loginUser)
  }

  @Test
  fun `resolveUser returns the same user when not azuread from mapping`() {
    val loginUser = User.builder().username("username").source(auth).build()
    val scopes = setOf("read", "delius")

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(loginUser)
  }

  @Test
  fun `resolveUser can map from azureAD to delius`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "emailid@email.com", "jwtId")
    val deliusUser = DeliusUserPersonDetails("username", "id", "user", "name", "email@email.com", true)
    val scopes = setOf("delius")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(deliusUser)

    verify(deliusUserService).getDeliusUsersByEmail("emailid@email.com")
  }

  @Test
  fun `resolveUser tries all three sources when no valid scopes found`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "emailid@email.com", "jwtId")
    val scopes = setOf("read,write")

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(loginUser)
    verify(deliusUserService).getDeliusUsersByEmail("emailid@email.com")
    verify(nomisUserService).getNomisUsersByEmail("emailid@email.com")
    verify(authUserService).findAuthUsersByEmail("emailid@email.com")
  }

  @Test
  fun `resolveUser can map from azureAD to nomis`() {
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

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(nomisUser)

    verify(nomisUserService).getNomisUsersByEmail("emailid@email.com")
  }

  @Test
  fun `resolveUser returns the same user when the user service returns no users`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val scopes = setOf("delius")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(emptyList())

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(loginUser)
  }

  @Test
  fun `resolveUser returns the same user when no to mapping exists`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val scopes = setOf("nomis")

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(loginUser)
  }

  @Test
  fun `resolveUser throws exception when multiple users matched`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val deliusUser = DeliusUserPersonDetails("username", "id", "user", "name", "email@email.com", true)
    val authUser = User.builder().username("username").source(auth).enabled(true).build()
    val scopes = setOf("delius", "auth")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser))

    assertThatThrownBy { userContextService.resolveUser(loginUser, scopes) }
      .isInstanceOf(UserMappingException::class.java)
      .hasMessage("Multiple users found with scopes $scopes")
  }

  @Test
  fun `resolveUser throws exception when multiple users matched from same source`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val authUser = User.builder().username("username").source(auth).enabled(true).build()
    val scopes = setOf("delius", "auth")
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser, authUser))

    assertThatThrownBy { userContextService.resolveUser(loginUser, scopes) }
      .isInstanceOf(UserMappingException::class.java)
      .hasMessage("Multiple users found with scopes $scopes")
  }

  @Test
  fun `resolveUser ignores disabled users`() {
    val loginUser = UserDetailsImpl("username", "name", listOf(), "azuread", "email@email.com", "jwtId")
    val deliusUser = DeliusUserPersonDetails(
      "username",
      "id",
      "user",
      "name",
      "email@email.com",
      enabled = false
    )
    val authUser = User.builder().username("username").source(auth).enabled(true).build()
    val scopes = setOf("delius", "auth")
    whenever(deliusUserService.getDeliusUsersByEmail(anyString())).thenReturn(listOf(deliusUser))
    whenever(authUserService.findAuthUsersByEmail(anyString())).thenReturn(listOf(authUser))

    val user = userContextService.resolveUser(loginUser, scopes)
    assertThat(user).isSameAs(authUser)
  }
}

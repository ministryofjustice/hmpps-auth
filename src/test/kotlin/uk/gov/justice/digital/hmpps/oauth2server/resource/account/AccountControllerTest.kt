package uk.gov.justice.digital.hmpps.oauth2server.resource.account

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.util.Base64Utils
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserHelper.Companion.createSampleUser
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.UserContextService
import java.util.Optional
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AccountControllerTest {
  private val request: HttpServletRequest = mock()
  private val response: HttpServletResponse = mock()
  private val userService: UserService = mock()
  private val userContextService: UserContextService = mock()
  private val backLinkHandler: BackLinkHandler = mock()
  private val accountController = AccountController(userService, userContextService, backLinkHandler)
  private val token = TestingAuthenticationToken(
    UserDetailsImpl("user", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"),
    "pass"
  )
  private val token2 = TestingAuthenticationToken(
    UserDetailsImpl("anemail@somewhere.com", "name", setOf(), AuthSource.auth.name, "userid", "jwtId"),
    "pass"
  )

  @Test
  fun `account details`() {
    val user = createSampleUser("master")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(user))
    val authUser = createSampleUser("build")
    whenever(userService.getUserWithContacts(anyString())).thenReturn(authUser)

    val modelAndView = accountController.accountDetails(null, null, token, request, response, "Lw==")

    assertThat(modelAndView.viewName).isEqualTo("account/accountDetails")
    assertThat(modelAndView.model).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "user" to user,
        "authUser" to authUser,
        "mfaPreferenceVerified" to false,
        "linkedAccounts" to emptyList<String>(),
        "canSwitchUsernameToEmail" to false,
        "usernameNotEmail" to true,
        "returnTo" to "/",
      )
    )
    verify(userService).findMasterUserPersonDetails("user")
    verify(userService).getUserWithContacts("user")
  }

  @Test
  fun `account details can switch username`() {
    val authUser = createSampleUser("build", email = "anemail@somewhere.com")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(authUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(authUser)

    val modelAndView = accountController.accountDetails(null, null, token, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("canSwitchUsernameToEmail", true)
  }

  @Test
  fun `account details username same as email UsernameNotEmail set to false`() {
    val authUser = createSampleUser("anemail@somewhere.com", email = "anemail@somewhere.com")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(authUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(authUser)

    val modelAndView = accountController.accountDetails(null, null, token2, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("usernameNotEmail", false)
  }

  @Test
  fun `account details cannot switch username as email not set`() {
    val authUser = createSampleUser("build")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(authUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(authUser)

    val modelAndView = accountController.accountDetails(null, null, token, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("canSwitchUsernameToEmail", false)
  }

  @Test
  fun `account details cannot switch username as already using email`() {
    val authUser = createSampleUser("build@joe", email = "anemail@somewhere.com")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(authUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(authUser)

    val modelAndView = accountController.accountDetails(null, null, token, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("canSwitchUsernameToEmail", false)
  }

  @Test
  fun `account details cannot switch username as email already taken`() {
    val authUser = createSampleUser("build", email = "anemail@somewhere.com")
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(authUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(authUser)
    whenever(userService.findUser(anyString())).thenReturn(Optional.of(authUser))

    val modelAndView = accountController.accountDetails(null, null, token, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("canSwitchUsernameToEmail", false)
  }

  @Test
  fun `account details cannot switch username for non auth users`() {
    val nomisUser = createSampleUser("build", email = "anemail@somewhere.com", source = AuthSource.nomis)
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(nomisUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(nomisUser)

    val modelAndView = accountController.accountDetails(null, null, token, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("canSwitchUsernameToEmail", false)
  }

  @Test
  fun `account details returnTo param used for backlink back to external service`() {
    val nomisUser = createSampleUser("build", email = "anemail@somewhere.com", source = AuthSource.nomis)
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(nomisUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(nomisUser)
    whenever(backLinkHandler.validateRedirect(anyString(), anyString())).thenReturn(true)

    val modelAndView = accountController.accountDetails("/somewhere-else/", "bob", token, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("returnTo", "/somewhere-else/")
  }

  @Test
  fun `account details returnTo param fails and slash for backlink`() {
    val nomisUser = createSampleUser("build", email = "anemail@somewhere.com", source = AuthSource.nomis)
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(nomisUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(nomisUser)
    whenever(backLinkHandler.validateRedirect(anyString(), anyString())).thenReturn(false)

    val modelAndView = accountController.accountDetails("/somewhere-not-valid/", "bob", token, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("returnTo", "/")
  }

  @Test
  fun `account details cookie value used for backlink back to external service`() {
    val nomisUser = createSampleUser("build", email = "anemail@somewhere.com", source = AuthSource.nomis)
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(nomisUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(nomisUser)
    val base64Cookie = Base64Utils.encodeToString("/somewhere-else/cookie".toByteArray())
    val modelAndView = accountController.accountDetails(null, null, token, request, response, base64Cookie)

    assertThat(modelAndView.model).containsEntry("returnTo", "/somewhere-else/cookie")
  }

  @Test
  fun `account details default cookie value used for backlink back to main menu`() {
    val nomisUser = createSampleUser("build", email = "anemail@somewhere.com", source = AuthSource.nomis)
    whenever(userService.findMasterUserPersonDetails(anyString())).thenReturn(Optional.of(nomisUser))
    whenever(userService.getUserWithContacts(anyString())).thenReturn(nomisUser)
    val modelAndView = accountController.accountDetails(null, null, token, request, response, "Lw==")

    assertThat(modelAndView.model).containsEntry("returnTo", "/")
  }
}

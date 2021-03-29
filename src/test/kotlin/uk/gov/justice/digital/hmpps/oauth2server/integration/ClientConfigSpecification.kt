package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentList
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ClientConfigSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var clientSummaryPage: ClientSummaryPage

  @Page
  private lateinit var clientMaintenancePage: ClientMaintenancePage

  @Page
  private lateinit var clientMaintenanceAddPage: ClientMaintenanceAddPage

  @Page
  private lateinit var duplicateClientSuccessPage: DuplicateClientSuccessPage

  @Page
  private lateinit var clientMaintenancePageWithError: ClientMaintenancePageWithError

  @Test
  fun `View Client Dashboard once logged in`() {
    goTo("/ui")
    loginPage.isAtPage().submitLogin("ITAG_USER_ADM", "password123456")

    clientSummaryPage.isAtPage()
      .checkClientSummary()
  }

  @Test
  fun `I can edit a client credential`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient()
    clientMaintenancePage.isAtPage().checkDetails().save()
    clientSummaryPage.isAtPage()
  }

  @Test
  fun `I can edit a client credential with extra jwt field`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient("elite2apiclient")
    clientMaintenancePage.isAtPage()
    assertThat(el("#jwtFields").value()).isEqualTo("-name")
  }

  @Test
  fun `I can edit a client credential with legacy username field`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient("prison-to-probation-update-api-client")
    clientMaintenancePage.isAtPage()
    assertThat(el("#databaseUsernameField").value()).isEqualTo("DSS_USER")
  }

  @Test
  fun `I can edit a client credential with an mfa field`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient("deliusnewtech")
    clientMaintenancePage.isAtPage()
    assertThat(el("#mfa-2").selected()).isTrue
  }

  @Test
  fun `I can edit a client credential and set an mfa field`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient("v1-client")
    clientMaintenancePage.isAtPage()
    assertThat(el("#mfa-3").selected()).isFalse
    clientMaintenancePage.selectCheckboxOption("mfa-3").save()
    clientSummaryPage.isAtPage().editClient("v1-client")
    assertThat(el("#mfa-3").selected()).isTrue
    clientMaintenancePage.selectCheckboxOption("mfa-1").save()
  }

  @Test
  fun `I can edit a client credential as an auth user`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient()
    clientMaintenancePage.isAtPage().checkDetails().save()
    clientSummaryPage.isAtPage()
  }

  @Test
  fun `I can edit a client and new details are copied over to the duplicate`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("rotation-test-client")
    clientMaintenancePage.isAtPage()
      .edit("registeredRedirectUri", "http://a_url:3003")
      .edit("accessTokenValiditySeconds", "1234")
      .edit("scopes", "read,bob")
      .save()
    clientSummaryPage.isAtPage()
    goTo(clientSummaryPage).editClient("rotation-test-client")
    with(clientMaintenancePage) {
      isAtPage()
      assertThat(el("#registeredRedirectUri").value()).isEqualTo("http://a_url:3003")
      assertThat(el("#accessTokenValiditySeconds").value()).isEqualTo("1234")
      assertThat(el("#scopes").value()).isEqualTo("read,bob")
      editClient("rotation-test-client-2")
      assertThat(el("#registeredRedirectUri").value()).isEqualTo("http://a_url:3003")
      assertThat(el("#accessTokenValiditySeconds").value()).isEqualTo("1234")
      assertThat(el("#scopes").value()).isEqualTo("read,bob")
    }
  }

  @Test
  fun `I can navigate between duplicate clients`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("rotation-test-client")
    with(clientMaintenancePage) {
      isAtPage()
      assertThat(el("[data-qa='other-clients']").text()).isEqualTo("Other clients")
      assertThat(el("#clientId").value()).isEqualTo("rotation-test-client")
      editClient("rotation-test-client-2")
      assertThat(el("#clientId").value()).isEqualTo("rotation-test-client-2")
      editClient("rotation-test-client")
      assertThat(el("#clientId").value()).isEqualTo("rotation-test-client")
    }
  }

  @Test
  fun `I can edit a client duplicate and new details are copied over to the original`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("rotation-test-client")
    clientMaintenancePage.isAtPage()
      .editClient("rotation-test-client-2")
      .edit("resourceIds", "some_resource")
      .edit("refreshTokenValiditySeconds", "2345")
      .edit("authorities", "ROLE_BOB,ROLE_JOE")
      .save()
    clientSummaryPage.isAtPage()
    goTo(clientSummaryPage).editClient("rotation-test-client")
    with(clientMaintenancePage) {
      isAtPage()
      assertThat(el("#resourceIds").value()).isEqualTo("some_resource")
      assertThat(el("#refreshTokenValiditySeconds").value()).isEqualTo("2345")
      assertThat(el("#authorities").value()).isEqualTo("ROLE_BOB,ROLE_JOE")
      editClient("rotation-test-client-2")
      assertThat(el("#resourceIds").value()).isEqualTo("some_resource")
      assertThat(el("#refreshTokenValiditySeconds").value()).isEqualTo("2345")
      assertThat(el("#authorities").value()).isEqualTo("ROLE_BOB,ROLE_JOE")
    }
  }

  @Test
  fun `I can create and remove client credential`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient(client = "client")
    clientMaintenanceAddPage.isAtPage()
      .edit("clientId", "new-client")
      .edit("clientSecret", "a-new-secret")
      .edit("registeredRedirectUri", "http://a_url:3003")
      .edit("accessTokenValiditySeconds", "1200")
      .edit("scopes", "read")
      .edit("authorities", "ROLE_BOB,ROLE_FRED")
      .selectCheckboxOption("client_credentials")
      .edit("jwtFields", "-name")
      .selectCheckboxOption("mfa-3")
      .save()
    clientSummaryPage.isAtPage()
      .checkClientSummary(
        client = "new-client",
        text =
        """
          new-client 
          [read] 
          [client_credentials] 
          [ROLE_BOB, ROLE_FRED] 
          1200 
          MFA ALL
      """
      )

    // now remove so test is re-runnable
    goTo("/ui/clients/new-client/delete")
    clientSummaryPage.isAtPage()
      .checkClientDoesntExist("new-client")
  }

  @Test
  fun `I can duplicate a client`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("rotation-test-client-2")
    clientMaintenancePage.isAtPage()
      .duplicate()

    duplicateClientSuccessPage.isAtPage()
      .continueToClientUiPage()

    // now remove so test is re-runnable
    goTo("/ui/clients/rotation-test-client-3/delete")
    clientSummaryPage.isAtPage()
      .checkClientDoesntExist("rotation-test-client-3")
  }

  @Test
  fun `I receive error if I try to have more than 3 of a client`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("rotation-test-client-2")
    clientMaintenancePage.isAtPage()
      .duplicate()

    duplicateClientSuccessPage.isAtPage()
      .continueToClientUiPage()

    goTo(clientSummaryPage).editClient("rotation-test-client-3")
    clientMaintenancePage.isAtPage()
      .duplicate()

    clientMaintenancePageWithError
      .checkError("You are only allowed 3 versions of this client at one time. You will need to delete one to be able to duplicate it again.")

    // now remove so test is re-runnable
    goTo("/ui/clients/rotation-test-client-3/delete")
    clientSummaryPage.isAtPage()
      .checkClientDoesntExist("rotation-test-client-3")
  }
}

@PageUrl("/ui")
class ClientSummaryPage : AuthPage<ClientSummaryPage>(
  "HMPPS Digital Services - Administration Dashboard",
  "OAuth server administration dashboard"
) {
  @FindBy(css = "table tbody tr")
  private lateinit var rows: FluentList<FluentWebElement>

  @Suppress("UsePropertyAccessSyntax")
  fun checkClientSummary(
    client: String = "apireporting",
    text: String =
      """
      apireporting 
      [reporting] 
      [client_credentials] 
      [ROLE_REPORTING] 
      3600""",
  ): ClientSummaryPage {
    assertThat(rows).hasSizeGreaterThan(10)
    assertThat(el("tr[data-qa='$client']").text()).isEqualTo(text.replaceIndent().replace("\n", ""))
    return this
  }

  fun checkClientDoesntExist(client: String) {
    assertThat(el("tr[data-qa='$client']").displayed()).isFalse()
  }

  fun editClient(client: String = "apireporting") {
    el("#edit-$client").click()
  }
}

@PageUrl("/ui/clients/form")
open class ClientMaintenancePage(heading: String = "Edit client", headingStartsWith: Boolean = true) :
  AuthPage<ClientMaintenancePage>(
    "HMPPS Digital Services - Maintain Client Configuration",
    heading,
    headingStartsWith
  ) {
  @FindBy(css = "input[type='submit']")
  private lateinit var save: FluentWebElement

  @FindBy(name = "duplicate-client")
  private lateinit var duplicate: FluentWebElement

  fun checkDetails(): ClientMaintenancePage {
    assertThat(el("#clientId").value()).isEqualTo("apireporting")
    assertThat(el("#clientSecret").value()).isBlank()
    assertThat(el("#accessTokenValiditySeconds").value()).isEqualTo("3600")
    assertThat(el("#authorities").value()).isEqualTo("ROLE_REPORTING")
    assertThat(el("#jwtFields").value()).isBlank()
    assertThat(el("#databaseUsernameField").value()).isBlank()
    return this
  }

  fun editClient(client: String = "apireporting"): ClientMaintenancePage {
    el("#edit-$client").click()
    return this
  }

  fun edit(field: String, text: String): ClientMaintenancePage {
    el("#$field").click().fill().with(text)
    return this
  }

  fun selectCheckboxOption(type: String): ClientMaintenancePage {
    el("#$type").click()
    return this
  }

  fun save() {
    save.click()
  }

  fun duplicate(): ClientMaintenancePage {
    duplicate.click()
    return this
  }
}

@PageUrl("/ui/clients/form")
class ClientMaintenanceAddPage : ClientMaintenancePage("Add client", false)

@PageUrl("/ui/clients/form")
class ClientMaintenancePageWithError : ClientMaintenancePage("Edit client 'rotation-test-client-3'", false)

@PageUrl("ui/clients/duplicate-client-success")
open class DuplicateClientSuccessPage : AuthPage<DuplicateClientSuccessPage>(
  "HMPPS Digital Services - Duplicate Client Configuration",
  "rotation-test-client-3"
) {
  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun continueToClientUiPage(): DuplicateClientSuccessPage {
    assertThat(continueButton.text()).isEqualTo("Continue")
    continueButton.click()
    return this
  }
}

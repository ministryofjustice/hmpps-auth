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
  private lateinit var clientCreatedSuccessPage: ClientCreatedSuccessPage

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
  fun `I can edit a client credential with Jira Ticket Number`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient("prison-to-probation-update-api-client")
    clientMaintenancePage.isAtPage()
    assertThat(el("#jiraNo").value()).isEqualTo("DT-2264")
  }

  @Test
  fun `when Jira Tick number entered url to jira is displayed next to input box`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient("prison-to-probation-update-api-client")
    clientMaintenancePage.isAtPage()
    assertThat(el("#jiraNo").value()).isEqualTo("DT-2264")
    assertThat(el("#jiraNoLink").text()).isEqualTo("https://dsdmoj.atlassian.net/browse/DT-2264")
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

    goTo(clientSummaryPage).editClient("rotation-test-client-1")
    clientMaintenancePage.isAtPage()
      .edit("registeredRedirectUri", "http://a_url:3003")
      .edit("accessTokenValiditySeconds", "1234")
      .edit("scopes", "read,bob")
      .save()
    clientSummaryPage.isAtPage()
    goTo(clientSummaryPage).editClient("rotation-test-client-1")
    with(clientMaintenancePage) {
      isAtPage()
      assertThat(el("#registeredRedirectUri").value()).isEqualTo("http://a_url:3003")
      assertThat(el("#accessTokenValiditySeconds").value()).isEqualTo("1234")
      assertThat(el("#scopes").value()).isEqualTo("read,bob")
    }
    goTo("/ui/clients/form?client=rotation-test-client-2")
    with(clientMaintenancePage) {
      assertThat(el("#registeredRedirectUri").value()).isEqualTo("http://a_url:3003")
      assertThat(el("#accessTokenValiditySeconds").value()).isEqualTo("1234")
      assertThat(el("#scopes").value()).isEqualTo("read,bob")
    }
  }

  @Test
  fun `I can edit a client duplicate and new details are copied over to the original`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo("/ui/clients/form?client=rotation-test-client-2")
    clientMaintenancePage.isAtPage()
      .edit("resourceIds", "some_resource")
      .edit("refreshTokenValiditySeconds", "2345")
      .edit("authorities", "  BOB\n\n, role_joe \n")
      .save()
    clientSummaryPage.isAtPage()
    goTo(clientSummaryPage).editClient("rotation-test-client-1")
    with(clientMaintenancePage) {
      isAtPage()
      assertThat(el("#resourceIds").value()).isEqualTo("some_resource")
      assertThat(el("#refreshTokenValiditySeconds").value()).isEqualTo("2345")
      assertThat(el("#authorities").value()).isEqualTo("BOB\nJOE")
    }
    goTo("/ui/clients/form?client=rotation-test-client-2")
    with(clientMaintenancePage) {
      assertThat(el("#resourceIds").value()).isEqualTo("some_resource")
      assertThat(el("#refreshTokenValiditySeconds").value()).isEqualTo("2345")
      assertThat(el("#authorities").value()).isEqualTo("BOB\nJOE")
    }
  }

  @Test
  fun `I can create and remove client credential`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient(client = "client")
    clientMaintenanceAddPage.isAtPage()
      .edit("clientId", "new-client")
      .edit("registeredRedirectUri", "http://a_url:3003")
      .edit("accessTokenValiditySeconds", "1200")
      .edit("scopes", "read")
      .edit("authorities", "  BOB\n\n, role_fred \n")
      .selectCheckboxOption("client_credentials")
      .edit("jwtFields", "-name")
      .edit("jiraNo", "DT-2264")
      .selectCheckboxOption("mfa-3")
      .save()
    clientCreatedSuccessPage.isAtPage()
      .checkClientSuccessDetails()
      .continueToClientPage()
    clientMaintenancePage.isAtPage()
      .cancelBackToUI()
    clientSummaryPage.isAtPage()
      .checkClientSummary(
        client = "new-client",
        text =
        """
          new-client 
          [read] 
          [client_credentials] 
          [BOB, FRED] 
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
  fun `I can create and remove client credential - create clientId ends with whitespace`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(clientSummaryPage).editClient(client = "client")
    clientMaintenanceAddPage.isAtPage()
      .edit("jiraNo", "DT-2264")
      .edit("clientId", "new-client  ")
      .edit("registeredRedirectUri", "http://a_url:3003")
      .edit("accessTokenValiditySeconds", "1200")
      .edit("scopes", "read")
      .edit("authorities", "  BOB\n\n, role_fred \n")
      .selectCheckboxOption("client_credentials")
      .edit("jwtFields", "-name")
      .selectCheckboxOption("mfa-3")
      .save()
    clientCreatedSuccessPage.isAtPage()
      .checkClientSuccessDetails()
      .continueToClientPage()
    clientMaintenancePage.isAtPage()
      .cancelBackToUI()
    clientSummaryPage.isAtPage()
      .checkClientSummary(
        client = "new-client",
        text =
        """
          new-client 
          [read] 
          [client_credentials] 
          [BOB, FRED] 
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

    goTo(clientSummaryPage).editClient("rotation-test-client-1")
    clientMaintenancePage.isAtPage()
      .duplicate()

    duplicateClientSuccessPage.isAtPage()
      .checkClientSuccessDetails()
      .continueToClientUiPage()

    // now remove so test is re-runnable
    goTo("/ui/clients/rotation-test-client-3/delete")
    clientSummaryPage.isAtPage()
      .checkClientDoesntExist("rotation-test-client-3")
  }

  @Test
  fun `Display last accessed, created and secret updated`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("rotation-test-client-1")
    with(clientMaintenancePage) {
      isAtPage()
      assertThat(el("#rotation-test-client-1-last-accessed").text()).isEqualTo("28-01-2013 13:23")
      assertThat(el("#rotation-test-client-1-secret-updated").text()).isEqualTo("27-01-2013 13:23")
      assertThat(el("#rotation-test-client-1-created").text()).isEqualTo("26-01-2013 13:23")
      assertThat(el("#rotation-test-client-2-last-accessed").text()).isEqualTo("25-12-2018 01:03")
      assertThat(el("#rotation-test-client-1-secret-updated").text()).isEqualTo("27-01-2013 13:23")
      assertThat(el("#rotation-test-client-2-created").text()).isEqualTo("25-12-2018 01:03")
    }
  }

  @Test
  fun `I receive error if I try to have more than 3 of a client`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("rotation-test-client")
    clientMaintenancePage.isAtPage()
      .duplicate()

    duplicateClientSuccessPage.isAtPage()
      .continueToClientUiPage()

    clientMaintenancePage.isAtPage()
      .duplicate()

    clientMaintenancePageWithError
      .checkError("You are only allowed 3 versions of this client at one time. You will need to delete one to be able to duplicate it again.")

    // now remove so test is re-runnable
    goTo("/ui/clients/rotation-test-client-3/delete")
    clientSummaryPage.isAtPage()
      .checkClientDoesntExist("rotation-test-client-3")
  }

  @Test
  fun `Client deployment details are displayed for hosting - cloud platform`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("service-client")
    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform()
  }

  @Test
  fun `Client deployment details are displayed for hosting - other`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("individual-client")
    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsOther()
  }

  @Test
  fun `Client deployment detail are not deleted when duplicate client is delete but duplicated exist`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("service-client")
    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform()
      .duplicate()

    goTo("/ui/clients/service-client-1/delete")

    goTo(clientSummaryPage).editClient("service-client")
    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform()
  }

  @Test
  fun `Client deployment detail are not deleted when original client is delete but duplicated exist`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("service-client")
    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform()
      .duplicate()

    goTo("/ui/clients/service-client/delete")

    goTo(clientSummaryPage).editClient("service-client")
    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform()
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
      [REPORTING] 
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
    val baseClient = client.replace(regex = "-[0-9]*$".toRegex(), replacement = "")
    el("#edit-$baseClient").click()
  }
}

@PageUrl("/ui/clients/form")
open class ClientMaintenancePage(heading: String = "Edit client", headingStartsWith: Boolean = true) :
  AuthPage<ClientMaintenancePage>(
    "HMPPS Digital Services - Maintain Client Configuration",
    heading,
    headingStartsWith
  ) {
  @FindBy(name = "client-submit")
  private lateinit var save: FluentWebElement

  @FindBy(name = "duplicate-client")
  private lateinit var duplicate: FluentWebElement

  @FindBy(css = "#cancel")
  private lateinit var cancelButton: FluentWebElement

  fun checkDetails(): ClientMaintenancePage {
    assertThat(el("#clientId").value()).isEqualTo("apireporting")
    assertThat(el("#clientSecret").value()).isBlank
    assertThat(el("#accessTokenValiditySeconds").value()).isEqualTo("3600")
    assertThat(el("#authorities").value()).isEqualTo("REPORTING")
    assertThat(el("#jwtFields").value()).isBlank
    assertThat(el("#databaseUsernameField").value()).isBlank
    return this
  }

  fun checkDeploymentDetailsCloudPlatform(team: String = "A Team"): ClientMaintenancePage {
    assertThat(el("#clientType").text()).isEqualTo("SERVICE")
    assertThat(el("#team").text()).isEqualTo(team)
    assertThat(el("#teamContact").text()).isEqualTo("A Team contact")
    assertThat(el("#teamSlack").text()).isEqualTo("A team slack")
    assertThat(el("#hosting").text()).isEqualTo("CLOUDPLATFORM")
    assertThat(el("#namespace").text()).isEqualTo("service-dev")
    assertThat(el("#deployment").text()).isEqualTo("service-deployment")
    assertThat(el("#secretName").text()).isEqualTo("service-secret")
    assertThat(el("#clientIdKey").text()).isEqualTo("API_CLIENT_ID")
    assertThat(el("#secretKey").text()).isEqualTo("API_CLIENT_SECRET")
    return this
  }

  fun checkDeploymentDetailsOther(): ClientMaintenancePage {
    assertThat(el("#clientType").text()).isEqualTo("PERSONAL")
    assertThat(el("#team").text()).isEqualTo("Bob")
    assertThat(el("#teamContact").text()).isEqualTo("Bob@digital.justice.gov.uk")
    assertThat(el("#teamSlack").text()).isEqualTo("bob slack")
    assertThat(el("#hosting").text()).isEqualTo("OTHER")

    assertThat(el("#namespace").displayed()).isFalse
    assertThat(el("#deployment").displayed()).isFalse
    assertThat(el("#secretName").displayed()).isFalse
    assertThat(el("#clientIdKey").displayed()).isFalse
    assertThat(el("#secretKey").displayed()).isFalse
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

  fun cancelBackToUI(): ClientMaintenancePage {
    cancelButton.click()
    return this
  }

  fun deploymentChange() {
    el("#deploymentChange").click()
  }

  fun generateClientSecret(client: String) {
    el("#generate-secret-$client").click()
  }
}

@PageUrl("/ui/clients/form")
class ClientMaintenanceAddPage : ClientMaintenancePage("Add client", false)

@PageUrl("/ui/clients/form")
class ClientMaintenancePageWithError : ClientMaintenancePage("Edit client 'rotation-test-client'", false)

@PageUrl("ui/clients/client-success")
open class ClientCreatedSuccessPage : AuthPage<ClientCreatedSuccessPage>(
  "HMPPS Digital Services - Client Configuration",
  "Client has been created"
) {
  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun checkClientSuccessDetails(): ClientCreatedSuccessPage {
    assertThat(el("[data-qa='clientId']").text()).isEqualTo("new-client")
    assertThat(el("[data-qa='clientSecret']").text()).isNotBlank
    assertThat(el("[data-qa='base64ClientId']").text()).isEqualTo("bmV3LWNsaWVudA==")
    assertThat(el("[data-qa='base64ClientSecret']").text()).isNotBlank
    return this
  }

  fun continueToClientPage(): ClientCreatedSuccessPage {
    assertThat(continueButton.text()).isEqualTo("Continue")
    continueButton.click()
    return this
  }
}

@PageUrl("ui/clients/duplicate-client-success")
open class DuplicateClientSuccessPage : AuthPage<DuplicateClientSuccessPage>(
  "HMPPS Digital Services - Duplicate Client Configuration",
  "Client has been duplicated"
) {
  @FindBy(css = "#continue")
  private lateinit var continueButton: FluentWebElement

  fun checkClientSuccessDetails(): DuplicateClientSuccessPage {
    assertThat(el("[data-qa='clientId']").text()).isEqualTo("rotation-test-client-3")
    assertThat(el("[data-qa='clientSecret']").text()).isNotBlank
    assertThat(el("[data-qa='base64ClientId']").text()).isEqualTo("cm90YXRpb24tdGVzdC1jbGllbnQtMw==")
    assertThat(el("[data-qa='base64ClientSecret']").text()).isNotBlank
    return this
  }

  fun continueToClientUiPage(): DuplicateClientSuccessPage {
    assertThat(continueButton.text()).isEqualTo("Continue")
    continueButton.click()
    return this
  }
}

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
    assertThat(el("#legacyUsernameField").value()).isEqualTo("DSS_USER")
  }

  @Test
  fun `I can edit a client credential as an auth user`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient()
    clientMaintenancePage.isAtPage().checkDetails().save()
    clientSummaryPage.isAtPage()
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
      .editGrantType("client_credentials")
      .edit("jwtFields", "-name")
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
          Edit
      """
      )

    // now remove so test is re-runnable
    goTo("/ui/clients/new-client/delete")
    clientSummaryPage.isAtPage()
      .checkClientDoesntExist("new-client")
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
      3600 
      Edit""",
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

  fun checkDetails(): ClientMaintenancePage {
    assertThat(el("#clientId").value()).isEqualTo("apireporting")
    assertThat(el("#clientSecret").value()).isBlank()
    assertThat(el("#accessTokenValiditySeconds").value()).isEqualTo("3600")
    assertThat(el("#authorities").value()).isEqualTo("ROLE_REPORTING")
    assertThat(el("#jwtFields").value()).isBlank()
    assertThat(el("#legacyUsernameField").value()).isBlank()
    return this
  }

  fun edit(field: String, text: String): ClientMaintenancePage {
    el("#$field").click().fill().with(text)
    return this
  }

  fun editGrantType(type: String): ClientMaintenancePage {
    el("#$type").click()
    return this
  }

  fun save() {
    el("input[type='submit']").click()
  }
}

@PageUrl("/ui/clients/form")
class ClientMaintenanceAddPage : ClientMaintenancePage("Add client", false)

package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ClientDeploymentSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var clientSummaryPage: ClientSummaryPage

  @Page
  private lateinit var clientMaintenancePage: ClientMaintenancePage

  @Page
  private lateinit var clientDeploymentMaintenancePage: ClientDeploymentMaintenancePage

  @Test
  fun `I can add client deployment details to an existing client`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("max-duplicate-client")
    clientMaintenancePage.isAtPage()
      .deploymentChange()

    clientDeploymentMaintenancePage.isAtPage()
      .addClientDeploymentDetails("A Team", "A Team contact", "A team slack", "rotation-dev", "rotation", "rotation", "API_CLIENT_ID", "API_CLIENT_SECRET")

    clientSummaryPage.isAtPage()
      .editClient("max-duplicate-client")
    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform()
  }

  @Test
  fun `I can and and update client deployment details for an existing client`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("max-duplicate-client")
    clientMaintenancePage.isAtPage()
      .deploymentChange()

    clientDeploymentMaintenancePage.isAtPage()
      .addClientDeploymentDetails("A Team", "A Team contact", "A team slack", "rotation-dev", "rotation", "rotation", "API_CLIENT_ID", "API_CLIENT_SECRET")

    clientSummaryPage.isAtPage()
      .editClient("max-duplicate-client")

    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform()
      .deploymentChange()

    clientDeploymentMaintenancePage.isAtPage()
      .changeTeamName("Changed Team")

    clientSummaryPage.isAtPage()
      .editClient("max-duplicate-client")

    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform("Changed Team")
      .deploymentChange()

    clientDeploymentMaintenancePage.isAtPage()
      .changeTeamName()

    clientSummaryPage.isAtPage()
      .editClient("max-duplicate-client")

    clientMaintenancePage.isAtPage()
      .checkDeploymentDetailsCloudPlatform()
  }

  @Test
  fun `client deployment details are displayed in input field for client with existing deployment details`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(clientSummaryPage).editClient("rotation-test-client")
    clientMaintenancePage.isAtPage()
      .deploymentChange()

    clientDeploymentMaintenancePage.isAtPage()
      .checkClientDeploymentDetails()
  }
}

@PageUrl("/ui/clients/deployment")
class ClientDeploymentMaintenancePage : AuthPage<ClientDeploymentMaintenancePage>(
  "HMPPS Digital Services - Maintain Client Deployment Configuration",
  "Client deployment information"
) {
  @FindBy(css = "input[type='submit']")
  private lateinit var saveButton: FluentWebElement

  @FindBy(css = "input[name='team']")
  private lateinit var team: FluentWebElement

  @FindBy(css = "input[name='teamContact']")
  private lateinit var teamContact: FluentWebElement

  @FindBy(css = "input[name='teamSlack']")
  private lateinit var teamSlack: FluentWebElement

  @FindBy(css = "input[name='namespace']")
  private lateinit var namespace: FluentWebElement

  @FindBy(css = "input[name='deployment']")
  private lateinit var deployment: FluentWebElement

  @FindBy(css = "input[name='secretName']")
  private lateinit var secretName: FluentWebElement

  @FindBy(css = "input[name='clientIdKey']")
  private lateinit var clientIdKey: FluentWebElement

  @FindBy(css = "input[name='secretKey']")
  private lateinit var secretKey: FluentWebElement

  fun addClientDeploymentDetails(
    team: String,
    teamContact: String,
    teamSlack: String,
    namespace: String,
    deployment: String,
    secretName: String,
    clientIdKey: String,
    secretKey: String,
  ): ClientDeploymentMaintenancePage {
    selectCheckboxOption("type")
    this.team.fill().withText(team)
    this.teamContact.fill().withText(teamContact)
    this.teamSlack.fill().withText(teamSlack)
    selectCheckboxOption("hosting")
    this.namespace.fill().withText(namespace)
    this.deployment.fill().withText(deployment)
    this.secretName.fill().withText(secretName)
    this.clientIdKey.fill().withText(clientIdKey)
    this.secretKey.fill().withText(secretKey)
    assertThat(saveButton.value()).isEqualTo("Save")
    saveButton.click()
    return this
  }

  fun changeTeamName(team: String = "A Team"): ClientDeploymentMaintenancePage {
    this.team.fill().withText(team)
    saveButton.click()
    return this
  }

  private fun selectCheckboxOption(type: String): ClientDeploymentMaintenancePage {
    el("#$type").click()
    return this
  }

  fun checkClientDeploymentDetails(): ClientDeploymentMaintenancePage {
    assertThat(el("#team").value()).isEqualTo("A Team")
    assertThat(el("#teamContact").value()).isEqualTo("A Team contact")
    assertThat(el("#teamSlack").value()).isEqualTo("A team slack")
    assertThat(el("#namespace").value()).isEqualTo("rotation-dev")
    assertThat(el("#deployment").value()).isEqualTo("rotation")
    assertThat(el("#secretName").value()).isEqualTo("rotation")
    assertThat(el("#clientIdKey").value()).isEqualTo("API_CLIENT_ID")
    assertThat(el("#secretKey").value()).isEqualTo("API_CLIENT_SECRET")
    return this
  }
}

package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentList
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ServicesSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var servicesSummaryPage: ServicesSummaryPage

  @Page
  private lateinit var servicesMaintenancePage: ServicesMaintenancePage

  @Page
  private lateinit var servicesMaintenanceAddPage: ServicesMaintenanceAddPage

  @Test
  fun `View Services Dashboard once logged in`() {
    goTo("/ui/services")
    loginPage.isAtPage().submitLogin("ITAG_USER_ADM", "password123456")

    servicesSummaryPage.isAtPage()
      .checkServicesSummary()
  }

  @Test
  fun `I can edit a service credential`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(servicesSummaryPage).editService(service = "HDC")
    servicesMaintenancePage.isAtPage()
      .checkDetails()
      .editEnabled(false)
      .editRoles("ROLE_BOB\nROLE_JOE")
      .save()
    servicesSummaryPage.isAtPage()
      .checkServicesSummary(
        service = "HDC",
        text =
          """
      Home Detention Curfew 
      Service for HDC Licences Creation and Approval 
      [ROLE_BOB, ROLE_JOE] 
      http://localhost:3003 / hdcdigitalservice@digital.justice.gov.uk 
      false Edit
      """
      )
      .editService()

    // now change detail back so test is re-runnable
    servicesMaintenancePage.isAtPage()
      .editEnabled(true)
      .editRoles("ROLE_LICENCE_CA\nROLE_LICENCE_RO\nROLE_LICENCE_DM")
      .save()
  }

  @Test
  fun `I can edit a service credential as an auth user`() {
    goTo(loginPage).loginAs("AUTH_ADM", "password123456")

    goTo(servicesSummaryPage).editService()
    servicesMaintenancePage.isAtPage().checkDetails().save()
    servicesSummaryPage.isAtPage()
  }

  @Test
  fun `I can create and remove service credential`() {
    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")

    goTo(servicesSummaryPage).editService(service = "service")
    servicesMaintenanceAddPage.isAtPage()
      .edit("code", "NEW")
      .edit("name", "A new service")
      .edit("description", "With a description")
      .edit("url", "http://a_url:3003")
      .editRoles("ROLE_SOME\nROLE_THING")
      .save()
    servicesSummaryPage.isAtPage()
      .checkServicesSummary(
        service = "NEW",
        text =
          """
      A new service 
      With a description 
      [ROLE_SOME, ROLE_THING] 
      http://a_url:3003 / no email set 
      false 
      Edit
      """
      )

    // now remove so test is re-runnable
    goTo("/ui/services/NEW/delete")
    servicesSummaryPage.isAtPage()
      .checkServiceDoesntExist("NEW")
  }
}

@PageUrl("/ui/services")
class ServicesSummaryPage :
  AuthPage<ServicesSummaryPage>("HMPPS Digital Services - Services Dashboard", "Services dashboard") {
  @FindBy(css = "table tbody tr")
  private lateinit var rows: FluentList<FluentWebElement>

  @Suppress("UsePropertyAccessSyntax")
  fun checkServicesSummary(
    service: String = "POM",
    text: String =
      """
      Allocate a POM Service 
      Allocate the appropriate offender manager to a prisoner 
      [ROLE_ALLOC_MGR] 
      https://moic.service.justice.gov.uk / https://moic.service.justice.gov.uk/help 
      true 
      Edit
 """,
  ): ServicesSummaryPage {
    assertThat(rows).hasSizeGreaterThan(10)
    assertThat(el("tr[data-qa='$service']").text()).isEqualTo(text.replaceIndent().replace("\n", ""))
    return this
  }

  fun checkServiceDoesntExist(service: String) {
    assertThat(el("tr[data-qa='$service']").displayed()).isFalse()
  }

  fun editService(service: String = "HDC") {
    el("#edit-$service").click()
  }
}

@PageUrl("/ui/services/form")
open class ServicesMaintenancePage(heading: String = "Edit service", headingStartsWith: Boolean = true) :
  AuthPage<ServicesMaintenancePage>(
    "HMPPS Digital Services - Manage Service Configuration",
    heading,
    headingStartsWith
  ) {
  fun checkDetails(): ServicesMaintenancePage {
    assertThat(el("#code").value()).isEqualTo("HDC")
    assertThat(el("#name").value()).isEqualTo("Home Detention Curfew")
    assertThat(el("#description").value()).isEqualTo("Service for HDC Licences Creation and Approval")
    assertThat(el("#authorisedRoles").value()).isEqualTo("ROLE_LICENCE_CA\nROLE_LICENCE_RO\nROLE_LICENCE_DM")
    assertThat(el("#url").value()).isEqualTo("http://localhost:3003")
    assertThat(el("#email").value()).isEqualTo("hdcdigitalservice@digital.justice.gov.uk")
    assertThat(el("#enabled").value()).isEqualTo("true")
    return this
  }

  fun edit(field: String, text: String): ServicesMaintenancePage {
    el("#$field").fill().with(text)
    return this
  }

  fun editRoles(roles: String): ServicesMaintenancePage {
    el("#authorisedRoles").fill().with(roles)
    return this
  }

  fun editEnabled(enabled: Boolean): ServicesMaintenancePage {
    if (enabled) el("#enabled").click()
    else el("#disabled").click()
    return this
  }

  fun save() {
    el("input[type='submit']").click()
  }
}

@PageUrl("/ui/services/form")
class ServicesMaintenanceAddPage : ServicesMaintenancePage("Add service", false)

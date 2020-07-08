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
//
//  @Page
//  private lateinit var servicesMaintenancePage: ServicesMaintenancePage

  @Test
  fun `View Services Dashboard once logged in`() {
    goTo("/ui/services")
    loginPage.isAtPage().submitLogin("ITAG_USER_ADM", "password123456")

    servicesSummaryPage.isAtPage()
        .checkServicesSummary()
  }
//
//  @Test
//  fun `I can edit a service credential`() {
//    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")
//
//    goTo(servicesSummaryPage).editService()
//    servicesMaintenancePage.isAtPage().checkDetails().save()
//    servicesSummaryPage.isAtPage()
//  }
//
//  @Test
//  fun `I can edit a service credential with extra jwt field`() {
//    goTo(loginPage).loginAs("ITAG_USER_ADM", "password123456")
//
//    goTo(servicesSummaryPage).editService("elite2apiservice")
//    servicesMaintenancePage.isAtPage()
//    assertThat(el("#jwtFields").value()).isEqualTo("-name")
//  }
//
//  @Test
//  fun `I can edit a service credential as an auth user`() {
//    goTo(loginPage).loginAs("AUTH_ADM", "password123456")
//
//    goTo(servicesSummaryPage).editService()
//    servicesMaintenancePage.isAtPage().checkDetails().save()
//    servicesSummaryPage.isAtPage()
//  }
}

@PageUrl("/ui/services")
class ServicesSummaryPage : AuthPage<ServicesSummaryPage>("HMPPS Digital Services - Services Dashboard", "Services dashboard") {
  @FindBy(css = "table tbody tr")
  private lateinit var rows: FluentList<FluentWebElement>

  @Suppress("UsePropertyAccessSyntax")
  fun checkServicesSummary(): ServicesSummaryPage {
    assertThat(rows).hasSizeGreaterThan(10)
    assertThat(rows[0].text()).isEqualTo("""
      Allocate a POM Service 
      Allocate the appropriate offender manager to a prisoner 
      [ROLE_ALLOC_MGR] 
      https://moic.service.justice.gov.uk / https://moic.service.justice.gov.uk/help 
      true
 """.replaceIndent().replace("\n", ""))
    return this
  }

//  fun editService(service: String = "apireporting") {
//    el("#edit-$service").click()
//  }
}
//
//@PageUrl("/ui/services/form")
//class ServicesMaintenancePage : AuthPage<ServicesMaintenancePage>("HMPPS Digital Services - Maintain Service Configuration", "Edit service", true) {
//
//  fun checkDetails(): ServicesMaintenancePage {
//    assertThat(el("#serviceId").value()).isEqualTo("apireporting")
//    assertThat(el("#serviceSecret").value()).isBlank()
//    assertThat(el("#accessTokenValiditySeconds").value()).isEqualTo("3600")
//    assertThat(el("#authorities").value()).isEqualTo("ROLE_REPORTING")
//    assertThat(el("#jwtFields").value()).isBlank()
//    return this
//  }
//
//  fun save() {
//    el("input[type='submit']").click()
//  }
//}

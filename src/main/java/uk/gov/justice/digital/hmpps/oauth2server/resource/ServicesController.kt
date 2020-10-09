package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService

@Controller
@RequestMapping("ui/services")
class ServicesController(
  private val authServicesService: AuthServicesService,
  private val telemetryClient: TelemetryClient,
) {

  @GetMapping
  fun userIndex() = ModelAndView("ui/services", "serviceDetails", authServicesService.list())

  @GetMapping("/form")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun showEditForm(@RequestParam(value = "code", required = false) code: String?): ModelAndView {
    val service = if (code != null) {
      authServicesService.getService(code)
    } else {
      Service()
    }
    return ModelAndView("ui/service", "service", service)
  }

  @PostMapping("/edit")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun editService(
    authentication: Authentication,
    @ModelAttribute service: Service,
    @RequestParam(value = "newService", required = false) newService: Boolean = false,
  ): String? {
    val userDetails = authentication.principal as UserPersonDetails
    val telemetryMap = mapOf("username" to userDetails.username, "code" to service.code)
    if (newService) {
      authServicesService.addService(service)
      telemetryClient.trackEvent("AuthServiceDetailsAdd", telemetryMap, null)
    } else {
      authServicesService.updateService(service)
      telemetryClient.trackEvent("AuthServiceDetailsUpdate", telemetryMap, null)
    }
    return "redirect:/ui/services"
  }

  @GetMapping("/{code}/delete")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun deleteService(authentication: Authentication, @PathVariable code: String): String? {
    val userDetails = authentication.principal as UserPersonDetails
    val telemetryMap = mapOf("username" to userDetails.username, "code" to code)
    authServicesService.removeService(code)
    telemetryClient.trackEvent("AuthServiceDetailsDeleted", telemetryMap, null)
    return "redirect:/ui/services"
  }
}

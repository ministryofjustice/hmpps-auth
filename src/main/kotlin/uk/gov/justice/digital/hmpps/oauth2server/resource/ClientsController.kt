@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthorityPropertyEditor
import uk.gov.justice.digital.hmpps.oauth2server.config.SplitCollectionEditor
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.DuplicateClientsException

@Controller
@RequestMapping("ui/clients")
class ClientsController(
  private val clientsDetailsService: JdbcClientDetailsService,
  private val clientService: ClientService,
  private val telemetryClient: TelemetryClient,
) {
  @InitBinder
  fun initBinder(binder: WebDataBinder) {
    binder.registerCustomEditor(MutableCollection::class.java, SplitCollectionEditor(MutableSet::class.java, ","))
    binder.registerCustomEditor(GrantedAuthority::class.java, AuthorityPropertyEditor())
  }

  @GetMapping("/form")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun showEditForm(@RequestParam(value = "client", required = false) clientId: String?, model: Model): String {
    val clientDetails: ClientDetails = if (clientId != null) {
      clientsDetailsService.loadClientByClientId(clientId)
    } else {
      BaseClientDetails()
    }
    model.addAttribute("clientDetails", clientDetails)
    return "ui/form"
  }

  @PostMapping("/edit")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun editClient(
    authentication: Authentication,
    @ModelAttribute clientDetails: AuthClientDetails,
    @RequestParam(value = "newClient", required = false) newClient: String?,
  ): String {
    val userDetails = authentication.principal as UserPersonDetails
    val telemetryMap = mapOf("username" to userDetails.username, "clientId" to clientDetails.clientId)

    if (newClient == null) {
      clientsDetailsService.updateClientDetails(clientDetails)
      telemetryClient.trackEvent("AuthClientDetailsUpdate", telemetryMap, null)
      clientService.findAndUpdateDuplicates(clientDetails.clientId)
    } else {
      clientsDetailsService.addClientDetails(clientDetails)
      telemetryClient.trackEvent("AuthClientDetailsAdd", telemetryMap, null)
    }
    if (clientDetails.clientSecret.isNotEmpty()) {
      clientsDetailsService.updateClientSecret(clientDetails.clientId, clientDetails.clientSecret)
      telemetryClient.trackEvent("AuthClientSecretUpdated", telemetryMap, null)
    }
    return "redirect:/ui"
  }

  @GetMapping("/{clientId}/delete")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun deleteClient(authentication: Authentication, @PathVariable clientId: String): String {
    val userDetails = authentication.principal as UserPersonDetails
    val telemetryMap = mapOf("username" to userDetails.username, "clientId" to clientId)
    clientsDetailsService.removeClientDetails(clientId)
    telemetryClient.trackEvent("AuthClientDetailsDeleted", telemetryMap, null)
    return "redirect:/ui"
  }

  @PostMapping("/duplicate")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun duplicateClient(
    authentication: Authentication,
    @RequestParam(value = "clientIdDuplicate", required = true) clientId: String,
  ): ModelAndView {

    return try {
      val duplicatedClientDetails: ClientDetails = clientService.duplicateClient(clientId)
      val userDetails = authentication.principal as UserPersonDetails
      val telemetryMap = mapOf("username" to userDetails.username, "clientId" to duplicatedClientDetails.clientId)
      telemetryClient.trackEvent("AuthClientDetailsDuplicated", telemetryMap, null)
      ModelAndView("ui/duplicateClientSuccess", "clientId", duplicatedClientDetails.clientId)
        .addObject("clientSecret", duplicatedClientDetails.clientSecret)
    } catch (e: DuplicateClientsException) {
      ModelAndView("redirect:/ui/clients/form", "client", clientId)
        .addObject("error", "maxDuplicates")
    }
  }

  // Unfortunately the getAdditionalInformation getter creates an unmodifiable map, so can't be used with web binder.
  // Have to therefore extend and create our own accessor instead.
  class AuthClientDetails : BaseClientDetails() {
    override fun setScope(scope: Collection<String>) {
      // always keep scopes and auto-approve scopes in sync.
      super.setScope(scope)
      super.setAutoApproveScopes(scope)
    }

    var jwtFields: String?
      get() = additionalInformation["jwtFields"] as String?
      set(jwtFields) {
        addAdditionalInformation("jwtFields", jwtFields)
      }
    var skipToAzureField: Boolean?
      get() = additionalInformation["skipToAzureField"] as Boolean?
      set(skipToAzure) {
        addAdditionalInformation("skipToAzureField", skipToAzure)
      }
    var databaseUsernameField: String?
      get() = additionalInformation["databaseUsernameField"] as String?
      set(databaseUsername) {
        addAdditionalInformation("databaseUsernameField", databaseUsername)
      }
    var mfa: MfaAccess?
      get() = additionalInformation["mfa"] as MfaAccess?
      set(mfa) {
        addAdditionalInformation("mfa", mfa)
      }
  }
}

enum class MfaAccess {
  none, untrusted, all
}

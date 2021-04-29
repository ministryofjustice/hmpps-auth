@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.provider.ClientRegistrationService
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.stereotype.Controller
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ClientDeployment
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthorityPropertyEditor
import uk.gov.justice.digital.hmpps.oauth2server.config.SplitCollectionEditor
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientDetailsWithCopies
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.DuplicateClientsException
import java.time.LocalDateTime
import java.util.Base64.getEncoder

@Controller
@RequestMapping("ui/clients")
class ClientsController(
  private val clientRegistrationService: ClientRegistrationService,
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
  fun showEditForm(@RequestParam(value = "client", required = false) baseClientId: String?): ModelAndView {
    return if (baseClientId != null) {
      val (clientDetails, clients) = clientService.loadClientWithCopies(baseClientId)
      val clientDeployment = clientService.loadClientDeploymentDetails(baseClientId) ?: ClientDeployment(baseClientId = baseClientId)
      ModelAndView("ui/form", "clientDetails", clientDetails)
        .addObject("clients", clients)
        .addObject("deployment", clientDeployment)
    } else {
      val (clientDetails, clients) = ClientDetailsWithCopies(BaseClientDetails(), emptyList())
      ModelAndView("ui/form", "clientDetails", clientDetails)
        .addObject("clients", clients)
    }
  }

  @GetMapping("deployment")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun showDeploymentForm(@RequestParam(value = "client", required = true) clientId: String): ModelAndView {
    val (clientDeployment, baseClientId) = clientService.getClientDeploymentDetailsAndBaseClientId(clientId)
    return ModelAndView("ui/deploymentForm", "baseClientId", baseClientId)
      .addObject("clientDeployment", clientDeployment ?: ClientDeployment(baseClientId = clientId))
  }

  @PostMapping("/deployment")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun addClientDeploymentDetails(
    authentication: Authentication,
    @ModelAttribute clientDeployment: ClientDeployment,
  ): ModelAndView {

    clientService.saveClientDeploymentDetails(clientDeployment)

    return ModelAndView("redirect:/ui")
  }

  @PostMapping("/add")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun addClient(
    authentication: Authentication,
    @ModelAttribute clientDetails: AuthClientDetails,
    @RequestParam(value = "newClient", required = false) newClient: String?,
  ): ModelAndView {
    clientDetails.clientId = clientDetails.clientId.trim()
    val userDetails = authentication.principal as UserPersonDetails
    val telemetryMap = mapOf("username" to userDetails.username, "clientId" to clientDetails.clientId)

    val clientSecret = clientService.addClient(clientDetails)

    telemetryClient.trackEvent("AuthClientDetailsAdd", telemetryMap, null)
    return ModelAndView("redirect:/ui/clients/client-success", "newClient", newClient ?: "false")
      .addObject("clientId", clientDetails.clientId)
      .addObject("clientSecret", clientSecret)
      .addObject("base64ClientId", getEncoder().encodeToString(clientDetails.clientId.toByteArray()))
      .addObject("base64ClientSecret", getEncoder().encodeToString(clientSecret.toByteArray()))
  }

  @PostMapping("/edit")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun editClient(
    authentication: Authentication,
    @ModelAttribute clientDetails: AuthClientDetails,
    @RequestParam(value = "newClient", required = false) newClient: String?,
  ): ModelAndView {
    val userDetails = authentication.principal as UserPersonDetails
    val telemetryMap = mapOf("username" to userDetails.username, "clientId" to clientDetails.clientId)

    clientRegistrationService.updateClientDetails(clientDetails)
    telemetryClient.trackEvent("AuthClientDetailsUpdate", telemetryMap, null)
    clientService.findAndUpdateDuplicates(clientDetails.clientId)

    return ModelAndView("redirect:/ui")
  }

  @GetMapping("/client-success")
  fun clientSuccess(
    @RequestParam(value = "newClient", required = true) newClient: String,
    @RequestParam(value = "clientId", required = true) clientId: String,
    @RequestParam(value = "clientSecret", required = true) clientSecret: String,
    @RequestParam(value = "base64ClientId", required = true) base64ClientId: String,
    @RequestParam(value = "base64ClientSecret", required = true) base64ClientSecret: String,
  ): ModelAndView =
    ModelAndView("ui/clientSuccess", "newClient", newClient)
      .addObject("clientId", clientId)
      .addObject("clientSecret", clientSecret)
      .addObject("base64ClientId", base64ClientId)
      .addObject("base64ClientSecret", base64ClientSecret)

  @GetMapping("/{clientId}/delete")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun deleteClient(authentication: Authentication, @PathVariable clientId: String): String {
    val userDetails = authentication.principal as UserPersonDetails
    val telemetryMap = mapOf("username" to userDetails.username, "clientId" to clientId)
    clientService.removeClient(clientId)
    telemetryClient.trackEvent("AuthClientDetailsDeleted", telemetryMap, null)
    return "redirect:/ui"
  }

  @GetMapping("/generate")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun newClientSecretPrompt(
    authentication: Authentication,
    @RequestParam(value = "client", required = true) clientId: String,
    @RequestParam(value = "last", required = true) lastAccessed: String,
  ):
    ModelAndView = ModelAndView("ui/generateSecretPrompt", "clientId", clientId)
      .addObject("lastAccessed", LocalDateTime.parse(lastAccessed))

  @PostMapping("/generate")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun generateNewClientSecret(
    authentication: Authentication,
    @RequestParam clientId: String,
  ):
    ModelAndView {
      val userDetails = authentication.principal as UserPersonDetails
      val telemetryMap = mapOf("username" to userDetails.username, "clientId" to clientId)

      val clientSecret = clientService.generateClientSecret(clientId)
      telemetryClient.trackEvent("AuthClientSecretUpdated", telemetryMap, null)
      return ModelAndView("redirect:/ui/clients/client-success", "newClient", "false")
        .addObject("clientId", clientId)
        .addObject("clientSecret", clientSecret)
        .addObject("base64ClientId", getEncoder().encodeToString(clientId.toByteArray()))
        .addObject("base64ClientSecret", getEncoder().encodeToString(clientSecret.toByteArray()))
    }

  @PostMapping("/duplicate")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  fun duplicateClient(
    authentication: Authentication,
    @RequestParam(value = "clientIdDuplicate", required = true) clientId: String,
  ): ModelAndView = try {
    val duplicatedClientDetails = clientService.duplicateClient(clientId)
    val userDetails = authentication.principal as UserPersonDetails
    val telemetryMap = mapOf("username" to userDetails.username, "clientId" to duplicatedClientDetails.clientId)
    telemetryClient.trackEvent("AuthClientDetailsDuplicated", telemetryMap, null)
    ModelAndView("redirect:/ui/clients/duplicate-client-success", "clientId", duplicatedClientDetails.clientId)
      .addObject("clientSecret", duplicatedClientDetails.clientSecret)
      .addObject("base64ClientId", getEncoder().encodeToString(duplicatedClientDetails.clientId.toByteArray()))
      .addObject("base64ClientSecret", getEncoder().encodeToString(duplicatedClientDetails.clientSecret.toByteArray()))
  } catch (e: DuplicateClientsException) {
    ModelAndView("redirect:/ui/clients/form", "client", clientId)
      .addObject("error", "maxDuplicates")
  }

  @GetMapping("/duplicate-client-success")
  fun duplicateClientSuccess(
    @RequestParam(value = "clientId", required = true) clientId: String,
    @RequestParam(value = "clientSecret", required = true) clientSecret: String,
    @RequestParam(value = "base64ClientId", required = true) base64ClientId: String,
    @RequestParam(value = "base64ClientSecret", required = true) base64ClientSecret: String,
  ): ModelAndView =
    ModelAndView("ui/duplicateClientSuccess", "clientId", clientId)
      .addObject("clientSecret", clientSecret)
      .addObject("base64ClientId", base64ClientId)
      .addObject("base64ClientSecret", base64ClientSecret)

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

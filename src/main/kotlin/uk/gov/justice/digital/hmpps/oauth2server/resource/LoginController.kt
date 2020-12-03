package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.apache.commons.lang3.StringUtils
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache
import java.util.Collections
import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.StreamSupport
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Suppress("DEPRECATION")
@Controller
class LoginController(
  clientRegistrationRepository: Optional<InMemoryClientRegistrationRepository>,
  private val cookieRequestCache: CookieRequestCache,
  private val clientDetailsService: ClientDetailsService
) {

  private val clientRegistrations: List<ClientRegistration>

  init {
    clientRegistrations = clientRegistrationRepository.map { registrations: InMemoryClientRegistrationRepository ->
      StreamSupport
        .stream(registrations.spliterator(), false)
        .filter { reg -> reg.redirectUriTemplate != null }
        .collect(Collectors.toList())
    }
      .orElse(emptyList())
  }

  @GetMapping("/login")
  fun loginPage(
    @RequestParam(required = false) error: String?,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): ModelAndView {
    val savedRequest = cookieRequestCache.getRequest(
      request,
      response
    )
    if (savedRequest != null && !clientRegistrations.isEmpty()) {
      val redirectUrl = UriComponentsBuilder.fromUriString(savedRequest.redirectUrl).build()
      val clientId = redirectUrl.queryParams.getFirst("client_id")
      val isOAuthLogin = redirectUrl.path.endsWith("/oauth/authorize") && clientId != null
      if (isOAuthLogin) {
        val clientDetails = clientDetailsService.loadClientByClientId(clientId)
        val skipToAzure = clientDetails.additionalInformation.getOrDefault("skipToAzureField", false) as Boolean
        if (skipToAzure) {
          val azureADClient = clientRegistrations[0].clientName
          return ModelAndView(
            "redirect:/oauth2/authorization/$azureADClient",
            Collections.singletonMap("oauth2Clients", clientRegistrations)
          )
        }
      }
    }
    val modelAndView = ModelAndView("login", Collections.singletonMap("oauth2Clients", clientRegistrations))
    // send bad request if password wrong so that browser won't offer to save the password
    if (StringUtils.isNotBlank(error)) {
      modelAndView.status = HttpStatus.BAD_REQUEST
    }
    return modelAndView
  }

  @GetMapping(value = ["/logout"])
  fun logoutPage(request: HttpServletRequest, response: HttpServletResponse): String {
    val auth = SecurityContextHolder.getContext().authentication
    if (auth != null) {
      SecurityContextLogoutHandler().logout(request, response, auth)
    }
    return "redirect:/login?logout"
  }

  @GetMapping("/access-denied")
  fun accessDenied(): String {
    return "error/access-denied"
  }
}

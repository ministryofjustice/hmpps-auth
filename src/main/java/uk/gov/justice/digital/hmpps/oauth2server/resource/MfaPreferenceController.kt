package uk.gov.justice.digital.hmpps.oauth2server.resource

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.MfaPreferenceType
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.service.MfaService

@Controller
class MfaPreferenceController(private val userService: UserService, private val mfaService: MfaService) {

  @GetMapping("/mfa-preference")
  fun mfaPreferenceRequest(authentication: Authentication): ModelAndView {
    val user = userService.getUserWithContacts(authentication.name)
    return ModelAndView("mfaPreference", "text", user.mobile)
        .addObject("email", user.email)
        .addObject("current", user.mfaPreference)
  }

  @PostMapping("/mfa-preference")
  fun mfaPreference(@RequestParam mfaPreference: MfaPreferenceType, authentication: Authentication): String {
    mfaService.updateUserMfaPreference(mfaPreference, authentication.name)
    return "redirect:/account-details"
  }
}


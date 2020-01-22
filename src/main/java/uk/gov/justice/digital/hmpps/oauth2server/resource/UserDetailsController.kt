package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler
import uk.gov.justice.digital.hmpps.oauth2server.security.UserDetailsImpl
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/user-details")
open class UserDetailsController(private val authUserService: AuthUserService,
                                 private val telemetryClient: TelemetryClient,
                                 private val jwtAuthenticationSuccessHandler: JwtAuthenticationSuccessHandler,
                                 private val userService: UserService) {
  @GetMapping
  open fun userDetails(authentication: Authentication): ModelAndView {
    val user = authUserService.getAuthUserByUsername(getUserName(authentication)).orElseThrow()
    with(user.person) {
      return ModelAndView("userDetails").addFirstAndLastName(firstName, lastName)
    }
  }

  @PostMapping
  open fun changeDetails(@RequestParam firstName: String?,
                         @RequestParam lastName: String?,
                         authentication: Authentication,
                         request: HttpServletRequest, response: HttpServletResponse): ModelAndView {

    val modelAndView = ModelAndView("userDetails").validateBlankInputs(firstName, lastName)
    if (modelAndView.modelMap.isNotEmpty()) {
      return modelAndView.addObject("error", true)
          .addFirstAndLastName(firstName, lastName)
    }

    return try {
      val username = getUserName(authentication)
      authUserService.amendUser(username, firstName, lastName)
      telemetryClient.trackEvent("UpdateName", mapOf("username" to username), null)

      // have to amend the token in the session as it will contain different user details
      val userPersonDetails = userService.findMasterUserPersonDetails(username).orElseThrow()
      val successToken = UsernamePasswordAuthenticationToken(userPersonDetails, null, userPersonDetails.authorities)
      jwtAuthenticationSuccessHandler.addAuthenticationToRequest(request, response, successToken)

      return ModelAndView("redirect:/")
    } catch (e: CreateUserException) {
      modelAndView.addObject("error_${e.field}", e.errorCode)
          .addObject("error", true)
          .addFirstAndLastName(firstName, lastName)
    }
  }

  private fun getUserName(authentication: Authentication) = (authentication.principal as UserDetailsImpl).username

  private fun ModelAndView.validateBlankInputs(firstName: String?, lastName: String?): ModelAndView {
    if (firstName.isNullOrBlank()) {
      addObject("error_firstName", "required")
    }
    if (lastName.isNullOrBlank()) {
      addObject("error_lastName", "required")
    }
    return this
  }

  private fun ModelAndView.addFirstAndLastName(firstName: String?, lastName: String?): ModelAndView =
      addObject("firstName", firstName).addObject("lastName", lastName)
}

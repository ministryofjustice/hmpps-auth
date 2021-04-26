package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.commons.text.WordUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.security.NomisUserService
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

@RestController
@Validated
@Api(tags = ["/api/prisonuser"])
@RequestMapping("/api/prisonuser")
class PrisonUserController(
  private val userService: UserService,
  private val nomisUserService: NomisUserService,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) {
  @GetMapping
  @PreAuthorize("hasRole('ROLE_USE_OF_FORCE')")
  @ApiOperation(
    value = "Find prison users by first and last names.",
    notes = "Find prison users by first and last names.",
    nickname = "Prison users",
    produces = "application/json"
  )
  @ApiResponses(value = [ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class)])
  fun prisonUsersByFirstAndLastName(
    @ApiParam(
      value = "The first name to match. Case insensitive.",
      required = true
    ) @RequestParam @NotEmpty firstName: String,
    @ApiParam(
      value = "The last name to match. Case insensitive",
      required = true
    ) @RequestParam @NotEmpty lastName: String,
  ): List<PrisonUser> = userService.findPrisonUsersByFirstAndLastNames(firstName, lastName)
    .map {
      PrisonUser(
        username = it.username,
        staffId = it.userId.toLongOrNull(),
        email = it.email,
        verified = it.verified,
        firstName = WordUtils.capitalizeFully(it.firstName),
        lastName = WordUtils.capitalizeFully(it.lastName),
        name = WordUtils.capitalizeFully("${it.firstName} ${it.lastName}"),
        activeCaseLoadId = it.activeCaseLoadId
      )
    }

  @PostMapping("/{username}/email")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN')")
  @ApiOperation(
    value = "Amend a prison user email address.",
    nickname = "amendUserEmail",
    consumes = "application/json",
    produces = "application/json",
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK"),
      ApiResponse(code = 400, message = "Bad request e.g. missing email address", response = ErrorDetail::class),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class),
    ]
  )
  fun amendUserEmail(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
    @Valid @RequestBody amendUser: AmendEmail,
    @ApiIgnore request: HttpServletRequest,
    @ApiIgnore authentication: Authentication,
  ): String? {
    val setPasswordUrl =
      request.requestURL.toString().replaceFirst("/api/prisonuser/.*".toRegex(), "/verify-email-confirm?token=")

    val userEmailAndUsername = nomisUserService.changeEmailAndRequestVerification(
      username = username,
      emailInput = amendUser.email,
      url = setPasswordUrl,
      emailType = User.EmailType.PRIMARY
    )

    log.info("Amend user succeeded for user {}", userEmailAndUsername.username)
    return if (smokeTestEnabled) userEmailAndUsername.link else ""
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

/**
 * copy of PrisonUserDto with added Swagger annotations.
 * Done to keep presentation layer detail out of the service layer.
 */
data class PrisonUser(
  @ApiModelProperty(required = true, example = "RO_USER_TEST")
  val username: String,
  @ApiModelProperty(required = true, example = "1234564789")
  val staffId: Long?,
  @ApiModelProperty(required = false, example = "ryanorton@justice.gov.uk")
  val email: String?,
  @ApiModelProperty(required = true, example = "true")
  val verified: Boolean,
  @ApiModelProperty(required = true, example = "Ryan")
  val firstName: String,
  @ApiModelProperty(required = true, example = "Orton")
  val lastName: String,
  @ApiModelProperty(required = true, example = "Ryan Orton")
  val name: String,
  @ApiModelProperty(required = false, example = "MDI")
  val activeCaseLoadId: String?
)

data class AmendEmail(
  @ApiModelProperty(required = true, value = "Email address", example = "nomis.user@someagency.justice.gov.uk")
  @field:NotBlank(message = "Email must not be blank")
  val email: String?,
)

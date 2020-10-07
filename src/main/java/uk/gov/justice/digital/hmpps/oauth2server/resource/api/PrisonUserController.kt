package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import lombok.AllArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import javax.validation.constraints.NotEmpty

@Slf4j
@RestController
@Api(tags = ["/api/prisonusers"])
@RequestMapping("/api/prisonusers")
@AllArgsConstructor
class PrisonUserController(private val userService: UserService) {
  @GetMapping
  @ApiOperation(value = "Find prison users by first and last names.", notes = "Find prison users by first and last names.", nickname = "Prison users", produces = "application/json")
  @ApiResponses(value = [
    ApiResponse(code = 200, message = "OK", response = PrisonUser::class, responseContainer = "List"),
    ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class)])
  fun prisonUsersByFirstAndLastName(
      @ApiParam(value = "The first name to match. Case insensitive.", required = true) @RequestParam @NotEmpty firstName: String,
      @ApiParam(value = "The last name to match. Case insensitive", required = true) @RequestParam @NotEmpty lastName: String
  ): List<PrisonUser> {
    return userService.findPrisonUsersByFirstAndLastNames(firstName, lastName)
        .map {
          PrisonUser(
              username = it.username,
              firstName = it.person?.firstName,
              lastName = it.person?.lastName,
              emailAddress = it.email,
              verified = it.isVerified)
        }
  }
}


data class PrisonUser(
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val emailAddress: String?,
    val verified: Boolean,
)


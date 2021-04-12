package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserFilter
import uk.gov.justice.digital.hmpps.oauth2server.model.EmailAddress
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail
import uk.gov.justice.digital.hmpps.oauth2server.model.UserRole
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.security.Principal
import java.time.LocalDateTime

@RestController
@Api(tags = ["/api/user"])
class UserController(private val userService: UserService) {

  @GetMapping("/api/user/me")
  @ApiOperation(
    value = "Current user detail.",
    notes = "Current user detail.",
    nickname = "getMyUserInformation",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = UserDetail::class),
      ApiResponse(code = 401, message = "Unauthorized", response = ErrorDetail::class)
    ]
  )
  fun me(@ApiIgnore principal: Principal): UserDetail {
    val user = userService.findMasterUserPersonDetails(principal.name)
    return user.map { UserDetail.fromPerson(it) }.orElse(UserDetail.fromUsername(principal.name))
  }

  @GetMapping("/api/user/me/roles")
  @ApiOperation(
    value = "List of roles for current user.",
    notes = "List of roles for current user.",
    nickname = "getMyRoles",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = UserRole::class, responseContainer = "List"),
      ApiResponse(code = 401, message = "Unauthorized", response = ErrorDetail::class)
    ]
  )
  fun myRoles(@ApiIgnore authentication: Authentication): Collection<UserRole> =
    authentication.authorities.map { UserRole(it!!.authority.substring(5)) } // remove ROLE_

  @GetMapping("/api/user/{username}")
  @ApiOperation(
    value = "User detail.",
    notes = "User detail.",
    nickname = "getUserDetails",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = UserDetail::class),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class)
    ]
  )
  fun user(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
  ): ResponseEntity<Any> {
    val user = userService.findMasterUserPersonDetails(username)
    return user.map { UserDetail.fromPerson(it) }
      .map { Any::class.java.cast(it) }
      .map { ResponseEntity.ok(it) }.orElse(notFoundResponse(username))
  }

  @GetMapping("/api/user/{username}/roles")
  @ApiOperation(
    value = "List of roles for user. Currently restricted to service specific roles: ROLE_INTEL_ADMIN or ROLE_PPM_USER_ADMIN.",
    nickname = "userRoles",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorized", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class)
    ]
  )
  @PreAuthorize(
    "hasAnyRole('ROLE_INTEL_ADMIN', 'ROLE_PPM_USER_ADMIN')"
  )
  fun userRoles(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
  ): Collection<UserRole> {
    val user = userService.findMasterUserPersonDetails(username)
      .orElseThrow { UsernameNotFoundException("Account for username $username not found") }
    return user.authorities.map { UserRole(it!!.authority.substring(5)) }.toSet() // remove ROLE_
  }

  @GetMapping("/api/user/{username}/email")
  @ApiOperation(
    value = "Email address for user",
    notes = "Verified email address for user",
    nickname = "getUserEmail",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = EmailAddress::class),
      ApiResponse(code = 204, message = "No content.  No verified email address found for user"),
      ApiResponse(
        code = 404,
        message = "User not found.  The user doesn't exist in auth so could have never logged in",
        response = ErrorDetail::class
      )
    ]
  )
  fun getUserEmail(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
  ): ResponseEntity<*> = userService
    .getOrCreateUser(username)
    .map { user: User ->
      if (user.verified) ResponseEntity.ok(EmailAddress(user)) else ResponseEntity.noContent().build<Any>()
    }
    .orElseGet { notFoundResponse(username) }

  @PostMapping("/api/user/email")
  @ApiOperation(
    value = "Email address for users",
    notes = """Verified email address for users.  Post version that accepts multiple email addresses.
        Requires ROLE_MAINTAIN_ACCESS_ROLES or ROLE_MAINTAIN_ACCESS_ROLES_ADMIN.""",
    nickname = "getUserEmails",
    consumes = "application/json",
    produces = "application/json"
  )
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES', 'ROLE_MAINTAIN_ACCESS_ROLES_ADMIN')")
  fun getUserEmails(
    @ApiParam(value = "List of usernames.", required = true) @RequestBody usernames: List<String>,
  ): List<EmailAddress> = userService
    .getOrCreateUsers(usernames)
    .filter { it.verified }
    .map { EmailAddress(it) }

  private fun notFoundResponse(username: String): ResponseEntity<Any?> = ResponseEntity.status(HttpStatus.NOT_FOUND)
    .body(ErrorDetail("Not Found", "Account for username $username not found", "username"))

  @GetMapping("/api/user/search")
  @ApiOperation(
    value = """
      Search for users in the Auth DB who match on partial first name, surname, username or email and return a pageable result set. 
      Optionally choose the authentication sources from any combination of auth, delius, nomis and azuread sources.
      It will default to AuthSource.auth if the authSources parameter is omitted.
      Provide the authSources as a list of values with the same name. e.g. ?authSources=nomis&authSources=delius&authSources=auth
      It will return users with the requested auth sources where they have authenticated against the auth service at least once.
      Note: User information held in the auth service may be out of date with the user information held in the source systems as
      their details will be as they were the last time that they authenticated.
    """,
    nickname = "searchForUsersInMultipleSourceSystems",
    produces = "application/json"
  )
  @ApiResponses(value = [ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class)])
  @ApiImplicitParams(
    ApiImplicitParam(
      name = "page",
      dataType = "int",
      paramType = "query",
      value = "Results page you want to retrieve (0..N)",
      example = "0",
      defaultValue = "0"
    ),
    ApiImplicitParam(
      name = "size",
      dataType = "int",
      paramType = "query",
      value = "Number of records per page.",
      example = "10",
      defaultValue = "10"
    ),
    ApiImplicitParam(
      name = "sort",
      dataType = "string",
      paramType = "query",
      value = "Sort column and direction, eg sort=lastName,desc"
    )
  )
  @PreAuthorize(
    "hasAnyRole('ROLE_INTEL_ADMIN', 'ROLE_PPM_USER_ADMIN')"
  )
  fun searchForUsersInMultipleSourceSystems(
    @ApiParam(
      value = "The username, email or name of the user.",
      example = "j smith"
    ) @RequestParam(required = false) name: String?,
    @ApiParam(value = "User status to find ACTIVE, INACTIVE or ALL. Defaults to ALL if omitted.") @RequestParam(
      required = false,
      defaultValue = "ALL"
    ) status: UserFilter.Status,
    @ApiParam(value = "List of auth sources to search [nomis|delius|auth|azuread]. Defaults to auth if omitted.") @RequestParam(
      required = false
    ) authSources: List<AuthSource>?,
    @PageableDefault(sort = ["Person.lastName", "Person.firstName"], direction = Sort.Direction.ASC) pageable: Pageable,
    @ApiIgnore authentication: Authentication,
  ): Page<AuthUserWithSource> =
    userService.searchUsersInMultipleSourceSystems(
      name,
      pageable,
      authentication.name,
      authentication.authorities,
      status,
      authSources
    )
      .map { AuthUserWithSource.fromUser(it) }
}

data class AuthUserWithSource(
  @ApiModelProperty(
    required = true,
    value = "User ID",
    example = "91229A16-B5F4-4784-942E-A484A97AC865",
    position = 1
  )
  val userId: String? = null,

  @ApiModelProperty(required = true, value = "Username", example = "authuser", position = 2)
  val username: String? = null,

  @ApiModelProperty(
    required = true,
    value = "Email address",
    example = "auth.user@someagency.justice.gov.uk",
    position = 3
  )
  val email: String? = null,

  @ApiModelProperty(required = true, value = "First name", example = "Auth", position = 4)
  val firstName: String? = null,

  @ApiModelProperty(required = true, value = "Last name", example = "User", position = 5)
  val lastName: String? = null,

  @ApiModelProperty(
    required = true,
    value = "Account is locked due to incorrect password attempts",
    example = "true",
    position = 6
  )
  val locked: Boolean = false,

  @ApiModelProperty(required = true, value = "Account is enabled", example = "false", position = 7)
  val enabled: Boolean = false,

  @ApiModelProperty(required = true, value = "Email address has been verified", example = "false", position = 8)
  val verified: Boolean = false,

  @ApiModelProperty(required = true, value = "Last time user logged in", example = "01/01/2001", position = 9)
  val lastLoggedIn: LocalDateTime? = null,

  @ApiModelProperty(required = true, value = "Authentication source", example = "delius", position = 10)
  val source: AuthSource = AuthSource.auth,

) {
  companion object {
    fun fromUser(user: User): AuthUserWithSource {
      return AuthUserWithSource(
        userId = user.id.toString(),
        username = user.username,
        email = user.email,
        firstName = user.firstName,
        lastName = user.person?.lastName,
        locked = user.locked,
        enabled = user.isEnabled,
        verified = user.verified,
        lastLoggedIn = user.lastLoggedIn,
        source = user.source,
      )
    }
  }
}

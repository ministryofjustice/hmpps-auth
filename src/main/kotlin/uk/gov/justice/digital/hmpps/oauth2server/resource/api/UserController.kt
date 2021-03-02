package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.model.EmailAddress
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.model.UserDetail
import uk.gov.justice.digital.hmpps.oauth2server.model.UserRole
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import java.security.Principal

@RestController
@Api(tags = ["/api/user"])
class UserController(private val userService: UserService) {

  @GetMapping("/api/user")
  @ApiOperation(
    value = "WIP - Search for a user in Auth and NOMIS.",
    notes = "WIP - Search for a user in Auth and NOMIS.",
    nickname = "getUsers",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = UserDetail::class, responseContainer = "List"),
      ApiResponse(code = 204, message = "No users found."),
      ApiResponse(code = 400, message = "Bad request", response = ErrorDetail::class),
      ApiResponse(code = 401, message = "Unauthorized", response = ErrorDetail::class)
    ]
  )
  fun getUsers(
    @ApiParam(value = "The email address of the user.") @RequestParam email: String?
  ): ResponseEntity<Any> {

    if (email.isNullOrEmpty()) {
      return ResponseEntity.badRequest()
        .body(ErrorDetail(BAD_REQUEST.reasonPhrase, "No email address provided to search", "email"))
    }

    val users = userService.findUsersByEmail(email).map { UserDetail.fromPerson(it) }

    return if (users.isEmpty()) ResponseEntity.noContent().build() else ResponseEntity.ok(users)
  }

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
  ): ResponseEntity<*> {
    return userService
      .getOrCreateUser(username)
      .map { user: User ->
        if (user.verified) ResponseEntity.ok(EmailAddress(user)) else ResponseEntity.noContent().build<Any>()
      }
      .orElseGet { notFoundResponse(username) }
  }

  private fun notFoundResponse(username: String): ResponseEntity<Any?> = ResponseEntity.status(HttpStatus.NOT_FOUND)
    .body(ErrorDetail("Not Found", "Account for username $username not found", "username"))
}

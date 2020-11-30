package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService.CreateUserException
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.VerifyEmailException
import uk.gov.service.notify.NotificationClientException
import java.time.LocalDateTime
import java.util.Optional
import javax.persistence.EntityNotFoundException
import javax.servlet.http.HttpServletRequest

@RestController
@Api(tags = ["/api/authuser"])
class AuthUserController(
  private val userService: UserService,
  private val authUserService: AuthUserService,
  private val authUserGroupService: AuthUserGroupService,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/api/authuser/{username}")
  @ApiOperation(
    value = "User detail.",
    notes = "User detail.",
    nickname = "getUserDetails",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = AuthUser::class),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class)
    ]
  )
  fun user(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
  ): ResponseEntity<Any?> {
    val user = authUserService.getAuthUserByUsername(username)
    return user.map { AuthUser.fromUser(it) }
      .map { Any::class.java.cast(it) }
      .map { ResponseEntity.ok(it) }
      .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundBody(username)))
  }

  @GetMapping("/api/authuser")
  @ApiOperation(
    value = "Search for a user.",
    notes = "Search for a user.",
    nickname = "searchForUser",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = AuthUser::class, responseContainer = "List"),
      ApiResponse(code = 204, message = "No users found."),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class)
    ]
  )
  fun searchForUser(
    @ApiParam(value = "The email address of the user.", required = true) @RequestParam email: String?,
  ): ResponseEntity<Any> {
    val users = authUserService.findAuthUsersByEmail(email).map { AuthUser.fromUser(it) }
    return if (users.isEmpty()) ResponseEntity.noContent().build() else ResponseEntity.ok(users)
  }

  @GetMapping("/api/authuser/search")
  @ApiOperation(
    value = "Search for a user.",
    nickname = "searchForUser",
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
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  fun searchForUser(
    @ApiParam(
      value = "The username, email or name of the user.",
      example = "j smith"
    ) @RequestParam(required = false) name: String?,
    @ApiParam(value = "The role codes of the user.") @RequestParam(required = false) roles: List<String>?,
    @ApiParam(value = "The group codes of the user.") @RequestParam(required = false) groups: List<String>?,
    @PageableDefault(sort = ["username"], direction = Sort.Direction.ASC) pageable: Pageable,
    @ApiIgnore authentication: Authentication,
  ): Page<AuthUser> =
    authUserService.findAuthUsers(name, roles, groups, pageable, authentication.name, authentication.authorities)
      .map { AuthUser.fromUser(it) }

  @GetMapping("/api/authuser/me/assignable-groups")
  @ApiOperation(
    value = "Get list of assignable groups.",
    notes = "Get list of groups that can be assigned by the current user.",
    nickname = "assignableGroups",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = AuthUserGroup::class, responseContainer = "List"),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class)
    ]
  )
  fun assignableGroups(@ApiIgnore authentication: Authentication): List<AuthUserGroup> {
    val groups = authUserGroupService.getAssignableGroups(authentication.name, authentication.authorities)
    return groups.map { AuthUserGroup(it) }
  }

  @PutMapping("/api/authuser/{username}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(
    value = "Create user.",
    notes = "Create user.",
    nickname = "createUser",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail::class),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 409, message = "User or email already exists.", response = ErrorDetail::class),
      ApiResponse(code = 500, message = "Server exception e.g. failed to call notify.", response = ErrorDetail::class)
    ]
  )
  @Throws(NotificationClientException::class)
  fun createUser(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String?,
    @ApiParam(value = "Details of the user to be created.", required = true) @RequestBody createUser: CreateUser,
    @ApiParam(value = "Enforce whether this email is unique in auth.") @RequestParam(required = false) enforceUniqueEmail: Boolean,
    @ApiIgnore request: HttpServletRequest,
    @ApiIgnore authentication: Authentication,
  ): ResponseEntity<Any> {
    val user =
      if (StringUtils.isNotBlank(username)) userService.findMasterUserPersonDetails(StringUtils.trim(username)) else Optional.empty<Any>()

    // check that we're not asked to create a user that is already in nomis, auth or delius
    if (user.isPresent) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorDetail("username.exists", "User $username already exists", "username"))
    }
    if (enforceUniqueEmail && authUserService.findAuthUsersByEmail(createUser.email).isNotEmpty()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorDetail("email.exists", "User ${createUser.email} already exists", "email"))
    }
    val mergedGroups = mutableSetOf<String>()
    if (createUser.groupCodes != null) {
      mergedGroups.addAll(createUser.groupCodes)
    }
    if (!createUser.groupCode.isNullOrBlank()) {
      mergedGroups.add(createUser.groupCode)
    }

    // new user
    return try {
      val setPasswordUrl = createInitialPasswordUrl(request)
      val resetLink = authUserService.createUser(
        StringUtils.trim(username),
        createUser.email,
        createUser.firstName,
        createUser.lastName,
        mergedGroups,
        setPasswordUrl,
        authentication.name,
        authentication.authorities
      )
      log.info("Create user succeeded for user {}", username)
      if (smokeTestEnabled) {
        ResponseEntity.ok(resetLink)
      } else ResponseEntity.noContent().build()
    } catch (e: CreateUserException) {
      log.info(
        "Create user failed for user {} for field {} with reason {}",
        username,
        e.field,
        e.errorCode
      )
      ResponseEntity.badRequest().body(
        ErrorDetail(
          String.format("%s.%s", e.field, e.errorCode),
          String.format("%s failed validation", e.field),
          e.field
        )
      )
    } catch (e: VerifyEmailException) {
      log.info("Create user failed for user {} for field email with reason {}", username, e.reason)
      ResponseEntity.badRequest()
        .body(ErrorDetail(String.format("email.%s", e.reason), "Email address failed validation", "email"))
    }
  }

  private fun createInitialPasswordUrl(@ApiIgnore request: HttpServletRequest): String {
    val requestURL = request.requestURL
    return requestURL.toString().replaceFirst("/api/authuser/.*".toRegex(), "/initial-password?token=")
  }

  @PutMapping("/api/authuser/{username}/enable")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(
    value = "Enable a user.",
    notes = "Enable a user.",
    nickname = "enableUser",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 204, message = "OK"),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(
        code = 403,
        message = "Unable to enable user, the user is not within one of your groups",
        response = ErrorDetail::class
      ),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class)
    ]
  )
  fun enableUser(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String?,
    @ApiIgnore authentication: Authentication,
  ): ResponseEntity<Any> {
    val userOptional = authUserService.getAuthUserByUsername(username)
    return userOptional.map { u: User ->
      val usernameInDb = u.username
      try {
        authUserService.enableUser(usernameInDb, authentication.name, authentication.authorities)
        return@map ResponseEntity.noContent().build<Any>()
      } catch (e: AuthUserGroupRelationshipException) {
        log.info("enable user failed  with reason {}", e.errorCode)
        return@map ResponseEntity.status(HttpStatus.FORBIDDEN).body<Any>(
          ErrorDetail(
            "unable to maintain user",
            "Unable to enable user, the user is not within one of your groups",
            "groups"
          )
        )
      } catch (e: EntityNotFoundException) {
        return@map ResponseEntity.notFound().build<Any>()
      }
    }.orElseThrow { EntityNotFoundException("User not found with username $username") }
  }

  @PutMapping("/api/authuser/{username}/disable")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(
    value = "Disable a user.",
    notes = "Disable a user.",
    nickname = "disableUser",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 204, message = "OK"), ApiResponse(
        code = 401,
        message = "Unauthorized.",
        response = ErrorDetail::class
      ), ApiResponse(
        code = 403,
        message = "Unable to disable user, the user is not within one of your groups",
        response = ErrorDetail::class
      ), ApiResponse(
        code = 404,
        message = "User not found.",
        response = ErrorDetail::class
      )
    ]
  )
  fun disableUser(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String?,
    @ApiIgnore authentication: Authentication,
  ): ResponseEntity<Any> {
    val userOptional = authUserService.getAuthUserByUsername(username)
    return userOptional.map { u: User ->
      val usernameInDb = u.username
      try {
        authUserService.disableUser(usernameInDb, authentication.name, authentication.authorities)
        return@map ResponseEntity.noContent().build<Any>()
      } catch (e: AuthUserGroupRelationshipException) {
        log.info("Disable user failed  with reason {}", e.errorCode)
        return@map ResponseEntity.status(HttpStatus.FORBIDDEN).body<Any>(
          ErrorDetail(
            "unable to maintain user",
            "Unable to disable user, the user is not within one of your groups",
            "groups"
          )
        )
      } catch (e: EntityNotFoundException) {
        return@map ResponseEntity.notFound().build<Any>()
      }
    }.orElseThrow { EntityNotFoundException("User not found with username $username") }
  }

  @PostMapping("/api/authuser/{username}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(
    value = "Amend a user.",
    notes = "Amend a user.",
    nickname = "amendUser",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 204, message = "OK"), ApiResponse(
        code = 400,
        message = "Bad request e.g. if validation failed or if the amendments are disallowed",
        response = ErrorDetail::class
      ), ApiResponse(
        code = 401,
        message = "Unauthorized.",
        response = ErrorDetail::class
      ), ApiResponse(
        code = 403,
        message = "Unable to amend user, the user is not within one of your groups",
        response = ErrorDetail::class
      ), ApiResponse(
        code = 404,
        message = "User not found.",
        response = ErrorDetail::class
      )
    ]
  )
  @Throws(
    NotificationClientException::class
  )
  fun amendUser(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
    @RequestBody amendUser: AmendUser,
    @ApiIgnore request: HttpServletRequest,
    @ApiIgnore authentication: Authentication,
  ): ResponseEntity<Any> {
    return try {
      val setPasswordUrl = createInitialPasswordUrl(request)
      val resetLink = authUserService.amendUserEmail(
        username,
        amendUser.email,
        setPasswordUrl,
        authentication.name,
        authentication.authorities,
        EmailType.PRIMARY
      )
      log.info("Amend user succeeded for user {}", username)
      if (smokeTestEnabled) {
        ResponseEntity.ok(resetLink)
      } else ResponseEntity.noContent().build()
    } catch (e: EntityNotFoundException) {
      ResponseEntity.notFound().build()
    } catch (e: VerifyEmailException) {
      log.info("Amend user failed for user {} for field email with reason {}", username, e.reason)
      ResponseEntity.badRequest()
        .body(ErrorDetail("email.${e.reason}", "Email address failed validation", "email"))
    } catch (e: AuthUserGroupRelationshipException) {
      log.info("enable user failed  with reason {}", e.errorCode)
      ResponseEntity.status(HttpStatus.FORBIDDEN).body(
        ErrorDetail(
          "unable to maintain user",
          "Unable to amend user, the user is not within one of your groups",
          "groups"
        )
      )
    }
  }

  data class CreateUser(
    @ApiModelProperty(
      required = true,
      value = "Email address",
      example = "nomis.user@someagency.justice.gov.uk",
      position = 1
    )
    val email: String?,

    @ApiModelProperty(required = true, value = "First name", example = "Nomis", position = 2)
    val firstName: String?,

    @ApiModelProperty(required = true, value = "Last name", example = "User", position = 3)
    val lastName: String?,

    @ApiModelProperty(value = "Initial group, required for group managers", example = "SITE_1_GROUP_1", position = 4)
    val groupCode: String?,

    @ApiModelProperty(
      value = "Initial groups, can be used if multiple initial groups required",
      example = "[\"SITE_1_GROUP_1\", \"SITE_1_GROUP_2\"]",
      position = 5
    )
    val groupCodes: Set<String>?,
  )

  data class AmendUser(
    @ApiModelProperty(required = true, value = "Email address", example = "nomis.user@someagency.justice.gov.uk")
    val email: String?,
  )

  data class AuthUser(
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
  ) {
    companion object {
      fun fromUser(user: User): AuthUser {
        return AuthUser(
          userId = user.id.toString(),
          username = user.username,
          email = user.email,
          firstName = user.firstName,
          lastName = user.person!!.lastName,
          locked = user.locked,
          enabled = user.isEnabled,
          verified = user.verified,
          lastLoggedIn = user.lastLoggedIn,
        )
      }
    }
  }

  private fun notFoundBody(username: String): Any =
    ErrorDetail("Not Found", "Account for username $username not found", "username")
}

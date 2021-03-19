package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupExistsException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserGroupManagerException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserGroupService.AuthUserLastGroupException
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException

@RestController
@Api(tags = ["/api/authuser/{username}/groups"])
class AuthUserGroupsController(
  private val authUserService: AuthUserService,
  private val authUserGroupService: AuthUserGroupService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/api/authuser/{username}/groups")
  @ApiOperation(
    value = "Get groups for user.",
    nickname = "groups",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class)
    ]
  )
  fun groups(
    @ApiParam(value = "The username of the user.", required = true)
    @PathVariable username: String,
    @ApiParam(value = "Whether groups are expanded into their children.", required = false)
    @RequestParam(defaultValue = "true") children: Boolean = true,
  ): List<AuthUserGroup> = authUserGroupService.getAuthUserGroups(username)
    ?.flatMap { g ->
      if (children && g.children.isNotEmpty()) g.children.map { AuthUserGroup(it) }
      else listOf(AuthUserGroup(g))
    }
    ?: throw UsernameNotFoundException("User $username not found")

  @PutMapping("/api/authuser/{username}/groups/{group}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(
    value = "Add group to user.",
    notes = "Add group to user.",
    nickname = "addGroup",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 204, message = "Created"),
      ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail::class),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class),
      ApiResponse(code = 409, message = "Group for user already exists.", response = ErrorDetail::class),
      ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail::class)
    ]
  )
  fun addGroup(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
    @ApiParam(value = "The group to be added to the user.", required = true) @PathVariable group: String,
    @ApiIgnore authentication: Authentication,
  ): ResponseEntity<Any> {
    val userOptional = authUserService.getAuthUserByUsername(username)
    return userOptional.map { it.username }
      .map { usernameInDb: String ->
        try {
          authUserGroupService.addGroup(usernameInDb, group, authentication.name, authentication.authorities)
          log.info("Add group succeeded for user {} and group {}", usernameInDb, group)
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
        } catch (e: AuthUserGroupExistsException) {
          log.info(
            "Add group failed for user {} for field {} with reason {}",
            usernameInDb,
            e.field,
            e.errorCode
          )
          return@map ResponseEntity.status(HttpStatus.CONFLICT).body<Any>(
            ErrorDetail(
              "group.exists",
              "Username $usernameInDb already has group $group",
              "group"
            )
          )
        } catch (e: AuthUserGroupManagerException) {
          log.info(
            "Add group failed for user {} and group {} for field {} with reason {}",
            usernameInDb,
            group,
            e.field,
            e.errorCode
          )
          return@map ResponseEntity.badRequest().body<Any>(
            ErrorDetail(
              "${e.field}.${e.errorCode}",
              "Group Manager is not a member of group",
              e.field
            )
          )
        } catch (e: AuthUserGroupException) {
          log.info(
            "Add group failed for user {} and group {} for field {} with reason {}",
            usernameInDb,
            group,
            e.field,
            e.errorCode
          )
          return@map ResponseEntity.badRequest().body<Any>(
            ErrorDetail(
              "${e.field}.${e.errorCode}",
              "${e.field} failed validation",
              e.field
            )
          )
        }
      }.orElse(notFoundResponse(username))
  }

  @DeleteMapping("/api/authuser/{username}/groups/{group}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(
    value = "Remove group from user.",
    notes = "Remove group from user.",
    nickname = "removeGroup",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 204, message = "Removed"),
      ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail::class),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class),
      ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail::class)
    ]
  )
  fun removeGroup(
    @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
    @ApiParam(value = "The group to be delete from the user.", required = true) @PathVariable group: String,
    @ApiIgnore authentication: Authentication,
  ): ResponseEntity<Any> {
    val userOptional = authUserService.getAuthUserByUsername(username)
    return userOptional.map { u: User ->
      val usernameInDb = u.username
      try {
        authUserGroupService.removeGroup(usernameInDb, group, authentication.name, authentication.authorities)
      } catch (e: AuthUserLastGroupException) {
        return@map ResponseEntity.status(HttpStatus.FORBIDDEN).body<Any>(
          ErrorDetail(
            "group.lastGroupRestriction",
            "Last group restriction, Group Manager not allowed to remove group: $group",
            "group"
          )
        )
      } catch (e: AuthUserGroupManagerException) {
        return@map ResponseEntity.status(HttpStatus.BAD_REQUEST).body<Any>(
          ErrorDetail(
            "group.managerNotMember",
            "Group Manager is not a member of group: $group",
            "group"
          )
        )
      } catch (e: AuthUserGroupException) {
        return@map ResponseEntity.status(HttpStatus.BAD_REQUEST).body<Any>(
          ErrorDetail(
            "group.missing",
            "Username $usernameInDb doesn't have the group $group",
            "group"
          )
        )
      }
      log.info("Remove group succeeded for user {} and group {}", usernameInDb, group)
      ResponseEntity.noContent().build()
    }.orElse(notFoundResponse(username))
  }

  private fun notFoundResponse(username: String): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.NOT_FOUND)
    .body(ErrorDetail("Not Found", "Account for username $username not found", "username"))
}

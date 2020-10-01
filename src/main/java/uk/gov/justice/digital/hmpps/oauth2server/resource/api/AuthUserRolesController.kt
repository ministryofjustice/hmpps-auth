package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import lombok.AllArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import javax.validation.constraints.NotEmpty

@Slf4j
@RestController
@Api(tags = ["/api/authuser/{username}/roles"])
@AllArgsConstructor
class AuthUserRolesController(
    private val authUserService: AuthUserService,
    private val authUserRoleService: AuthUserRoleService) {

  @GetMapping("/api/authuser/{username}/roles")
  @ApiOperation(value = "Get roles for user.", notes = "Get roles for user.", nickname = "roles", consumes = "application/json", produces = "application/json")
  @ApiResponses(value = [ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
    ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class)])
  fun roles(@ApiParam(value = "The username of the user.", required = true) @PathVariable username: String?): Set<AuthUserRole> {
    val user = authUserService.getAuthUserByUsername(username).orElseThrow { UsernameNotFoundException("Account for username $username not found") }
    return user.authorities.map { AuthUserRole(it) }.toSet()
  }

  @GetMapping("/api/authuser/{username}/assignable-roles")
  @ApiOperation(value = "Get list of assignable roles.", notes = "Get list of roles that can be assigned by the current user.  This is dependent on the group membership, although super users can assign any role", nickname = "assignableRoles", consumes = "application/json", produces = "application/json")
  @ApiResponses(value = [ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
    ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class)])
  fun assignableRoles(@ApiParam(value = "The username of the user.", required = true) @PathVariable username: String?,
                      @ApiIgnore authentication: Authentication): List<AuthUserRole> {
    val roles = authUserRoleService.getAssignableRoles(username, authentication.authorities)
    return roles.map { AuthUserRole(it) }
  }

  @PutMapping("/api/authuser/{username}/roles/{role}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(value = "Add role to user.", notes = "Add role to user.", nickname = "addRole", consumes = "application/json", produces = "application/json")
  @ApiResponses(value = [
    ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail::class),
    ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
    ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class),
    ApiResponse(code = 409, message = "Role for user already exists.", response = ErrorDetail::class),
    ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail::class)])
  fun addRole(
      @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
      @ApiParam(value = "The role to be added to the user.", required = true) @PathVariable role: String,
      @ApiIgnore authentication: Authentication) {
    val user = authUserService.getAuthUserByUsername(username).orElseThrow { UsernameNotFoundException("Account for username $username not found") }
    val usernameInDb = user.username
    authUserRoleService.addRoles(usernameInDb, listOf(role), authentication.name, authentication.authorities)
    log.info("Add role succeeded for user {} and role {}", usernameInDb, role)
  }

  @DeleteMapping("/api/authuser/{username}/roles/{role}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(value = "Remove role from user.", notes = "Remove role from user.", nickname = "removeRole", consumes = "application/json", produces = "application/json")
  @ApiResponses(value = [ApiResponse(code = 204, message = "Removed"),
    ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail::class),
    ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
    ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class),
    ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail::class)])
  fun removeRole(
      @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String?,
      @ApiParam(value = "The role to be delete from the user.", required = true) @PathVariable role: String?,
      @ApiIgnore authentication: Authentication) {
    val user = authUserService.getAuthUserByUsername(username).orElseThrow { UsernameNotFoundException("Account for username $username not found") }
    val usernameInDb = user.username
    authUserRoleService.removeRole(usernameInDb, role, authentication.name, authentication.authorities)
    log.info("Remove role succeeded for user {} and role {}", usernameInDb, role)
  }

  @PostMapping("/api/authuser/{username}/roles")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(value = "Add roles to user.", notes = "Add role to user, post version taking multiple roles", nickname = "addRole", consumes = "application/json", produces = "application/json")
  @ApiResponses(value = [
    ApiResponse(code = 400, message = "Validation failed.", response = ErrorDetail::class),
    ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
    ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class),
    ApiResponse(code = 409, message = "Role(s) for user already exists.", response = ErrorDetail::class),
    ApiResponse(code = 500, message = "Server exception e.g. failed to insert row.", response = ErrorDetail::class)])
  fun addRole(
      @ApiParam(value = "The username of the user.", required = true) @PathVariable username: String,
      @ApiParam(value = "List of roles to be assigned.", required = true) @RequestBody @NotEmpty roles: List<String>,
      @ApiIgnore authentication: Authentication) {
    val user = authUserService.getAuthUserByUsername(username).orElseThrow { UsernameNotFoundException("Account for username $username not found") }
    val usernameInDb = user.username
    authUserRoleService.addRoles(usernameInDb, roles, authentication.name, authentication.authorities)
    log.info("Add role succeeded for user {} and roles {}", usernameInDb, roles.toString())
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

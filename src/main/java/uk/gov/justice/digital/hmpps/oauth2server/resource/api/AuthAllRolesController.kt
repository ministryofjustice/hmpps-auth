package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserRoleService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail

@RestController
@Api(tags = ["/api/authroles"])
class AuthAllRolesController(private val authUserRoleService: AuthUserRoleService) {
  @GetMapping("/api/authroles")
  @ApiOperation(
    value = "Get all possible roles.",
    notes = "Get all roles allowed for auth users.",
    nickname = "allroles",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(value = [ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class)])
  fun allRoles(): List<AuthUserRole> =
    authUserRoleService.allRoles.map { AuthUserRole(it) }
}

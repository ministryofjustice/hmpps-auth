package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.maintain.GroupsService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserRole
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail

@RestController
class GroupsController(
  private val groupsService: GroupsService
) {

  @GetMapping("/api/groups/{group}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(
    value = "Group detail.",
    notes = "Group detail.",
    nickname = "getGroupDetails",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "Group not found.", response = ErrorDetail::class)
    ]
  )
  fun getGroupDetail(
    @ApiParam(value = "The group code of the group.", required = true)
    @PathVariable group: String,
    @ApiIgnore authentication: Authentication,
  ): GroupDetails {
    val returnedGroup: Group = groupsService.getGroupDetail(group, authentication.name, authentication.authorities)
    return GroupDetails(returnedGroup)
  }
}

@ApiModel(description = "Group Details")
data class GroupDetails(
  @ApiModelProperty(required = true, value = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @ApiModelProperty(required = true, value = "Group Name", example = "HDC NPS North East")
  val groupName: String,

  @ApiModelProperty(required = true, value = "Assignable Roles")
  val assignableRoles: List<AuthUserRole>,

  @ApiModelProperty(required = true, value = "Child Groups")
  val children: List<AuthUserGroup>,
) {
  constructor(g: Group) : this(
    g.groupCode,
    g.groupName,
    g.assignableRoles.map { AuthUserRole(it.role) }.sortedBy { it.roleName },
    g.children.map { AuthUserGroup(it) }.sortedBy { it.groupName }
  )
}

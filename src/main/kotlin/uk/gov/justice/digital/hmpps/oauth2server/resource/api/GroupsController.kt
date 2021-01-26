package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ChildGroup
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import uk.gov.justice.digital.hmpps.oauth2server.maintain.GroupsService
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserAssignableRole
import uk.gov.justice.digital.hmpps.oauth2server.model.AuthUserGroup
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@Validated
@RestController
class GroupsController(
  private val groupsService: GroupsService
) {

  @GetMapping("/api/groups/{group}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ApiOperation(
    value = "Group detail.",
    nickname = "getGroupDetails",
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

  @GetMapping("/api/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @ApiOperation(
    value = "Child Group detail.",
    nickname = "getChildGroupDetails",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "Child Group not found.", response = ErrorDetail::class)
    ]
  )
  fun getChildGroupDetail(
    @ApiParam(value = "The group code of the child group.", required = true)
    @PathVariable group: String,
    @ApiIgnore authentication: Authentication,
  ): ChildGroupDetails {
    val returnedGroup: ChildGroup =
      groupsService.getChildGroupDetail(group, authentication.name, authentication.authorities)
    return ChildGroupDetails(returnedGroup)
  }

  @PutMapping("/api/groups/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @ApiOperation(
    value = "Amend group name.",
    nickname = "AmendGroupName",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "Group not found.", response = ErrorDetail::class)
    ]
  )
  fun amendGroupName(
    @ApiParam(value = "The group code of the group.", required = true)
    @PathVariable group: String,
    @ApiIgnore authentication: Authentication,
    @ApiParam(
      value = "Details of the group to be updated.",
      required = true
    ) @RequestBody groupAmendment: GroupAmendment,

  ) {
    groupsService.updateGroup(authentication.name, group, groupAmendment)
  }

  @PutMapping("/api/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @ApiOperation(
    value = "Amend child group name.",
    nickname = "AmendChildGroupName",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "Child Group not found.", response = ErrorDetail::class)
    ]
  )
  fun amendChildGroupName(
    @ApiParam(value = "The group code of the child group.", required = true)
    @PathVariable group: String,
    @ApiIgnore authentication: Authentication,
    @ApiParam(
      value = "Details of the child group to be updated.",
      required = true
    ) @RequestBody groupAmendment: GroupAmendment,

  ) {
    groupsService.updateChildGroup(authentication.name, group, groupAmendment)
  }

  @PostMapping("/api/groups/child")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @ApiOperation(
    value = "Create child group.",
    nickname = "CreateChildGroup",
    consumes = "application/json",
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 409, message = "Child Group already exists.", response = ErrorDetail::class)
    ]
  )
  @Throws(GroupsService.ChildGroupExistsException::class, GroupsService.GroupNotFoundException::class)
  fun createChildGroup(
    @ApiParam(value = "The group code of the child group.", required = true)
    @ApiIgnore authentication: Authentication,
    @ApiParam(value = "Details of the child group to be created.", required = true)
    @Valid @RequestBody createChildGroup: CreateChildGroup,
  ) {
    groupsService.createChildGroup(authentication.name, createChildGroup)
  }

  @DeleteMapping("/api/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @ApiOperation(
    value = "Delete child group.",
    nickname = "DeleteChildGroup",
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "Child Group not found.", response = ErrorDetail::class)
    ]
  )
  fun deleteChildGroup(
    @ApiParam(value = "The group code of the child group.", required = true)
    @PathVariable group: String,
    @ApiIgnore authentication: Authentication,
  ) {
    groupsService.deleteChildGroup(authentication.name, group)
  }
}

@ApiModel(description = "Group Details")
data class GroupDetails(
  @ApiModelProperty(required = true, value = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @ApiModelProperty(required = true, value = "Group Name", example = "HDC NPS North East")
  val groupName: String,

  @ApiModelProperty(required = true, value = "Assignable Roles")
  val assignableRoles: List<AuthUserAssignableRole>,

  @ApiModelProperty(required = true, value = "Child Groups")
  val children: List<AuthUserGroup>,
) {
  constructor(g: Group) : this(
    g.groupCode,
    g.groupName,
    g.assignableRoles.map { AuthUserAssignableRole(it.role, it.automatic) }.sortedBy { it.roleName },
    g.children.map { AuthUserGroup(it) }.sortedBy { it.groupName }
  )
}

@ApiModel(description = "Group Name")
data class GroupAmendment(
  @ApiModelProperty(required = true, value = "Group Name", example = "HDC NPS North East")
  @field:NotBlank(message = "parent group code must be supplied")
  val groupName: String,
)

@ApiModel(description = "Group Details")
data class ChildGroupDetails(
  @ApiModelProperty(required = true, value = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @ApiModelProperty(required = true, value = "Group Name", example = "HDC NPS North East")
  val groupName: String,
) {
  constructor(g: ChildGroup) : this(
    g.groupCode,
    g.groupName,
  )
}

data class CreateChildGroup(
  @ApiModelProperty(required = true, value = "Parent Group Code", example = "HNC_NPS", position = 1)
  @field:NotBlank(message = "parent group code must be supplied")
  val parentGroupCode: String,

  @ApiModelProperty(required = true, value = "Group Code", example = "HDC_NPS_NE", position = 2)
  @field:NotBlank(message = "group code must be supplied")
  val groupCode: String,

  @ApiModelProperty(required = true, value = "groupName", example = "HDC NPS North East", position = 3)
  @field:NotBlank(message = "group name must be supplied")
  val groupName: String,
)

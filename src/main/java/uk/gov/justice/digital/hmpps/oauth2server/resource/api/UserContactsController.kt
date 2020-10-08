package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import lombok.AllArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService

@Slf4j
@RestController
@Api(tags = ["/api/user/{username}/contacts"])
@AllArgsConstructor
class UserContactsController(private val userService: UserService) {
  @GetMapping("/api/user/{username}/contacts")
  @PreAuthorize("hasRole('ROLE_RETRIEVE_OAUTH_CONTACTS')")
  @ApiOperation(
    value = "Get contacts for user.",
    notes = "Get verified contacts for user.",
    nickname = "contacts",
    consumes = "application/json",
    produces = "application/json"
  )
  @ApiResponses(
    value = [
      ApiResponse(code = 200, message = "OK", response = ContactDto::class, responseContainer = "List"),
      ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
      ApiResponse(code = 404, message = "User not found.", response = ErrorDetail::class)
    ]
  )
  fun contacts(
    @ApiParam(
      value = "The username of the user.",
      required = true
    ) @PathVariable username: String
  ): List<ContactDto> {
    val user = userService.getUserWithContacts(username)
    return user.contacts.filter { it.verified }.map { ContactDto(it.value!!, it.type.name, it.type.description) }
  }
}

data class ContactDto(
  @ApiModelProperty(required = true, example = "01234 23451234")
  val value: String,
  @ApiModelProperty(required = true, example = "SECONDARY_EMAIL")
  val type: String,
  @ApiModelProperty(required = true, example = "Mobile Phone")
  val typeDescription: String
)

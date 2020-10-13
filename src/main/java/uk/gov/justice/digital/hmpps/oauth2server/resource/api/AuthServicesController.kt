package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import lombok.extern.slf4j.Slf4j
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.model.ErrorDetail
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService

@Slf4j
@RestController
@Api(tags = ["/api/services"])
class AuthServicesController(private val authServicesService: AuthServicesService) {
  @GetMapping("/api/services")
  @ApiOperation(value = "Get all enabled services.", nickname = "services", produces = "application/json")
  @ApiResponses(value = [ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class)])
  fun services(): List<AuthService> = authServicesService.list().filter { it.isEnabled }.map { AuthService(it) }
}

@ApiModel(description = "Digital Services")
data class AuthService(
  @ApiModelProperty(required = true, example = "NOMIS")
  val code: String,

  @ApiModelProperty(required = true, example = "Digital Prison Services")
  val name: String,

  @ApiModelProperty(required = true, example = "View and Manage Offenders in Prison (Old name was NEW NOMIS)")
  val description: String?,

  @ApiModelProperty(required = true, example = "ROLE_PRISON")
  val roles: String?,

  @ApiModelProperty(required = true, example = "https://digital-dev.prison.service.justice.gov.uk")
  val url: String?,
) {
  constructor(s: Service) : this(s.code, s.name, s.description, s.authorisedRoles, s.url)
}

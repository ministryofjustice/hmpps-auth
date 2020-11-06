package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.service.AuthServicesService

@RestController
@Api(tags = ["/api/services"])
class AuthServicesController(private val authServicesService: AuthServicesService) {
  @GetMapping("/api/services")
  @ApiOperation(value = "Get all enabled services.", nickname = "services", produces = "application/json")
  fun services(): List<AuthService> = authServicesService.listEnabled().map { AuthService(it) }

  @GetMapping("/api/services/me")
  @ApiOperation(value = "Get my services.", nickname = "my services", produces = "application/json")
  fun myServices(authentication: Authentication): List<AuthService> =
    authServicesService.listEnabled(authentication.authorities).map { AuthService(it) }
}

@ApiModel(description = "Digital Services")
data class AuthService(
  @ApiModelProperty(required = true, example = "NOMIS")
  val code: String,

  @ApiModelProperty(required = true, example = "Digital Prison Services")
  val name: String,

  @ApiModelProperty(
    required = false,
    value = "Description of service, often blank",
    example = "View and Manage Offenders in Prison (Old name was NEW NOMIS)"
  )
  val description: String?,

  @ApiModelProperty(
    required = false,
    value = "Contact information, can be blank",
    example = "feedback@digital.justice.gov.uk"
  )
  val contact: String?,

  @ApiModelProperty(
    required = false,
    value = "URL of service",
    example = "https://digital-dev.prison.service.justice.gov.uk"
  )
  val url: String?,
) {
  constructor(s: Service) : this(s.code, s.name, s.description, s.email, s.url)
}

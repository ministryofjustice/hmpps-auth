@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.NoSuchClientException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.DuplicateClientsException

@Validated
@RestController
class DuplicateClientController(
  private val clientService: ClientService
) {

  @PutMapping("/api/client/{clientId}")
  @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
  // @ApiOperation(
  //   value = "Duplicate Client",
  //   nickname = "DuplicateClient",
  //   produces = "application/json"
  // )
  // @ApiResponses(
  //   value = [
  //     ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
  //     ApiResponse(code = 404, message = "client not found.", response = ErrorDetail::class)
  //   ]
  // )
  @Throws(DuplicateClientsException::class, NoSuchClientException::class)
  fun duplicateClient(
    @ApiIgnore authentication: Authentication,
    @PathVariable clientId: String,
  ): DuplicateClientDetail {
    val client = clientService.duplicateClient(clientId)

    return DuplicateClientDetail(client)
  }
}

// @ApiModel(description = "Duplicate Client Details")
data class DuplicateClientDetail(
  // @ApiModelProperty(required = true, value = "Client ID", example = "SERVICE-NAME-CLIENT")
  val clientId: String,

  // @ApiModelProperty(required = true, value = "Client Secret", example = ";4j9LcDk4<mRE/<TU8)v'-qP0")
  val ClientSecret: String,
) {
  constructor(c: ClientDetails) : this(
    c.clientId,
    c.clientSecret,
  )
}

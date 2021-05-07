@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.NoSuchClientException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientDuplicateIdsAndDeployment
import uk.gov.justice.digital.hmpps.oauth2server.service.ClientService
import uk.gov.justice.digital.hmpps.oauth2server.service.DuplicateClientsException
import java.util.Base64.getEncoder

@Validated
@RestController
class DuplicateClientController(
  private val clientService: ClientService,
  private val telemetryClient: TelemetryClient,
) {

  @GetMapping("/api/client/{clientId}")
  @PreAuthorize("hasRole('ROLE_CLIENT_ROTATION_ADMIN')")
  // @ApiOperation(
  //   value = "get Client",
  //   nickname = "GetClient"
  //   produces = "application/json"
  // )
  // @ApiResponses(
  //   value = [
  //     ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
  //     ApiResponse(code = 404, message = "client not found.", response = ErrorDetail::class)
  //   ]
  // )
  @Throws(NoSuchClientException::class)
  fun getClientIdsAndDeployment(
    @ApiIgnore authentication: Authentication,
    @PathVariable clientId: String,
  ): ClientDuplicateIdsAndDeployment {
    return clientService.loadClientAndDeployment(clientId)
  }

  @PutMapping("/api/client/{clientId}")
  @PreAuthorize("hasRole('ROLE_CLIENT_ROTATION_ADMIN')")
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
    val username = authentication.principal
    val telemetryMap = mapOf("username" to username.toString(), "clientId" to clientId)
    val client = clientService.duplicateClient(clientId)

    telemetryClient.trackEvent("AuthClientDetailsApiDuplicated", telemetryMap, null)
    return DuplicateClientDetail(client)
  }

  @DeleteMapping("/api/client/{clientId}")
  @PreAuthorize("hasRole('ROLE_CLIENT_ROTATION_ADMIN')")
  // @ApiOperation(
  //   value = "Delete Client",
  //   nickname = "Delete"
  // )
  // @ApiResponses(
  //   value = [
  //     ApiResponse(code = 401, message = "Unauthorized.", response = ErrorDetail::class),
  //     ApiResponse(code = 404, message = "client not found.", response = ErrorDetail::class)
  //   ]
  // )
  @Throws(NoSuchClientException::class)
  fun deleteClient(
    @ApiIgnore authentication: Authentication,
    @PathVariable clientId: String,
  ) {
    val username = authentication.principal
    val telemetryMap = mapOf("username" to username.toString(), "clientId" to clientId)
    clientService.removeClient(clientId)

    telemetryClient.trackEvent("AuthClientDetailsApiDeleted", telemetryMap, null)
  }
}

// @ApiModel(description = "Duplicate Client Details")
data class DuplicateClientDetail(
  // @ApiModelProperty(required = true, value = "Client ID", example = "SERVICE-NAME-CLIENT")
  val clientId: String,

  // @ApiModelProperty(required = true, value = "Client Secret", example = ";4j9LcDk4<mRE/<TU8)v'-qP0")
  val clientSecret: String,

  // @ApiModelProperty(required = true, value = "Base64 Client ID", example = "U0VSVklDRS1OQU1FLUNMSUVOVA==")
  val base64ClientId: String,

  // @ApiModelProperty(required = true, value = "Base64 Client Secret", example = "OzRqOUxjRGs0PG1SRS88VFU4KXYnLXFQMA==")
  val base64ClientSecret: String,
) {
  constructor(c: ClientDetails) : this(
    c.clientId,
    c.clientSecret,
    getEncoder().encodeToString(c.clientId.toByteArray()),
    getEncoder().encodeToString(c.clientSecret.toByteArray()),
  )
}

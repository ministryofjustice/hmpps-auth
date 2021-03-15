package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.servlet.http.HttpServletResponse

@RestController
class AuthServerMetadataController {

  @GetMapping("/issuer/.well-known/openid-configuration")
  fun metadata(response: HttpServletResponse): Map<String, Any> {
    val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
    response.addHeader("Cache-Control", "max-age=43200, public")
    return mapOf(
      "issuer" to "$baseUrl/issuer",
      "authorization_endpoint" to "$baseUrl/oauth/authorize",
      "token_endpoint" to "$baseUrl/oauth/token",
      "jwks_uri" to "$baseUrl/.well-known/jwks.json"
    )
  }
}

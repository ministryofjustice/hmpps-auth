package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import com.nimbusds.jose.jwk.JWKSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class JwkSetController(@Autowired private val jwkSet: JWKSet) {
  private val jwkSetJson = jwkSet.toJSONObject()

  @GetMapping("/.well-known/jwks.json")
  fun keys(response: HttpServletResponse): Map<String, Any> {
    response.addHeader("Cache-Control", "max-age=43200, public")
    return jwkSetJson
  }
}

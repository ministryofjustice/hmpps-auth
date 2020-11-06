package uk.gov.justice.digital.hmpps.oauth2server.resource

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import org.apache.commons.lang3.StringUtils
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.HashMap

/**
 * Takes the static token responses, incorporates the nonce from the client request into the ID token, and then signs them
 */
class TokenSignerTransformer : ResponseDefinitionTransformer() {
  private val mapper = ObjectMapper()
  private val typeRef = object : TypeReference<HashMap<String, Any>>() {}

  override fun getName(): String = "token-signer"

  override fun applyGlobally(): Boolean = false

  override fun transform(
    request: Request,
    responseDefinition: ResponseDefinition,
    files: FileSource,
    parameters: Parameters,
  ): ResponseDefinition {

    val accessToken = mapper.readValue(parameters.readJson("accessToken"), typeRef)
    val idToken = mapper.readValue(parameters.readJson("idToken"), typeRef)
    parameters["email"]?.let { idToken["preferred_username"] = it }

    // Incorporate the nonce value with that supplied in the original authorize request.
    // The nonce is sent back to the client as the "code" in response to the authorize request,
    // which would not normally happen, but means the mock server does not have to be stateful)
    getFormValues(request.bodyAsString)["code"]?.let { idToken["nonce"] = it }

    val rsaJWK = RSAKey.parse(parameters.readJson("privateKey").readText())

    val jsonResponse = mapOf(
      "token_type" to "bearer",
      "access_token" to getSignedToken(rsaJWK, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(accessToken)),
      "id_token" to getSignedToken(rsaJWK, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(idToken)),
    )

    return ResponseDefinitionBuilder()
      .withHeader("Content-Type", "application/json")
      .withStatus(200)
      .withBody(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse))
      .build()
  }

  @Throws(JOSEException::class)
  private fun getSignedToken(rsaJWK: RSAKey, token: String): String? {
    val signer: JWSSigner = RSASSASigner(rsaJWK)

    val jwsObject = JWSObject(
      JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.keyID).build(),
      Payload(token)
    )

    jwsObject.sign(signer)
    return jwsObject.serialize()
  }

  private fun getFormValues(requestBody: String): Map<String, String> {
    val valueMap = HashMap<String, String>()
    if (StringUtils.isNotEmpty(requestBody) && (requestBody.contains("&") || requestBody.contains("="))) {
      requestBody.split("&")
        .map { it.split("=") }
        .forEach { valueMap[it[0]] = if (it.size > 1) URLDecoder.decode(it[1], StandardCharsets.UTF_8.name()) else "" }
    }
    return valueMap
  }

  internal fun Parameters.readJson(parameterName: String): URL =
    this@TokenSignerTransformer::class.java.getResource(this.get(parameterName) as String)
}

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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

class TokenSignerTransformer : ResponseDefinitionTransformer() {
    private val mapper = ObjectMapper()
    private val typeRef = object : TypeReference<HashMap<String, Any>>() {}

    override fun getName(): String = "token-signer"

    override fun applyGlobally(): Boolean = false

    override fun transform(request: Request?, responseDefinition: ResponseDefinition?, files: FileSource?, parameters: Parameters?): ResponseDefinition {

        val accessToken: Map<String, Any> = mapper.readValue(this.javaClass.getResource(parameters?.get("accessToken") as String?), typeRef)
        val idToken: MutableMap<String, Any> = mapper.readValue(this.javaClass.getResource(parameters?.get("idToken") as String?), typeRef)

        //replace nonce value with that supplied in original auth request (and round tripped back as the "code")
        val nonce = getFormValues(request!!.bodyAsString)["code"]

        if (nonce != null) {
            idToken["nonce"] = nonce
        }

        //sign both tokens
        val rsaJWK = RSAKey.parse(this.javaClass.getResource(parameters?.get("privateKey") as String?).readText())

        val jsonResponse = HashMap<String, String?>()
        jsonResponse["token_type"] = "bearer"
        jsonResponse["access_token"] = getSignedToken(rsaJWK, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(accessToken))
        jsonResponse["id_token"] = getSignedToken(rsaJWK, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(idToken))

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
                Payload(token))

        jwsObject.sign(signer)
        return jwsObject.serialize()
    }

    private fun getFormValues(requestBody: String) : Map<String, String> {
        val valueMap = HashMap<String, String>()
        if (StringUtils.isNotEmpty(requestBody) && (requestBody.contains("&") || requestBody.contains("="))) {
                    requestBody.split("&")
                    .map { it.split("=") }
                    .forEach { valueMap[it[0]] = if (it.size > 1) URLDecoder.decode(it[1], StandardCharsets.UTF_8.name()) else "" }
        }
        return valueMap
    }
}
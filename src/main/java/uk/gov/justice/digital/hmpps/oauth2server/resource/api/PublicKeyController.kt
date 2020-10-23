package uk.gov.justice.digital.hmpps.oauth2server.resource.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.apache.commons.codec.binary.Base64
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.PublicKey

@Api(tags = ["jwt-public-key"])
@RestController
class PublicKeyController @Autowired constructor(
  @Value("\${jwt.signing.key.pair}") privateKeyPair: String?,
  @Value("\${jwt.keystore.password}") keystorePassword: String,
  @Value("\${jwt.keystore.alias:elite2api}") keystoreAlias: String?,
) {
  private val publicKey: PublicKey

  @RequestMapping(value = ["jwt-public-key"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ApiOperation(value = "Public JWT Key", notes = "formatted and base 64 encoded version", nickname = "getFormattedKey")
  fun getJwtPublicKey(): Map<String, Any> {
    val formattedKey = getFormattedKey(publicKey)
    return mapOf(
      "formatted" to convertNewLinesToArray(formattedKey),
      "encoded" to Base64.encodeBase64String(formattedKey.toByteArray())
    )
  }

  private fun convertNewLinesToArray(formattedKey: String): Array<String> =
    formattedKey.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

  private fun getFormattedKey(pk: PublicKey): String {
    val builder = StringBuilder()
    val encodeBase64String = Base64.encodeBase64String(pk.encoded)
    builder.append("-----BEGIN PUBLIC KEY-----")
    builder.append("\n")
    var i = 0
    while (i < encodeBase64String.length) {
      builder.append(encodeBase64String, i, Math.min(i + 64, encodeBase64String.length))
      builder.append("\n")
      i += 64
    }
    builder.append("-----END PUBLIC KEY-----")
    builder.append("\n")
    return builder.toString()
  }

  init {
    val keyStoreKeyFactory = KeyStoreKeyFactory(
      ByteArrayResource(Base64.decodeBase64(privateKeyPair)),
      keystorePassword.toCharArray()
    )
    publicKey = keyStoreKeyFactory.getKeyPair(keystoreAlias).public
  }
}

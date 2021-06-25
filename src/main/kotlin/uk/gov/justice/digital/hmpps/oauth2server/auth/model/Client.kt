@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.oauth2server.resource.MfaAccess
import java.io.IOException
import java.time.LocalDateTime
import javax.persistence.AttributeConverter
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Converter
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Suppress("JpaDataSourceORMInspection", "JpaAttributeTypeInspection")
@Entity
@Table(name = "oauth_client_details")
data class Client(
  @Id
  @Column(name = "client_id", nullable = false)
  val id: String,

  @Column(name = "additional_information")
  @Convert(converter = JsonToMapConverter::class)
  private val additionalInformation: Map<String, Any> = emptyMap(),

  @Convert(converter = StringListConverter::class)
  val authorities: List<String> = emptyList(),

  @Column(name = "authorized_grant_types", nullable = false)
  @Convert(converter = StringListConverter::class)
  val authorizedGrantTypes: List<String> = emptyList(),

  @Convert(converter = StringListConverter::class)
  val scope: List<String> = emptyList(),

  @Column(name = "access_token_validity")
  val accessTokenValidity: Int? = 0,

  @Column(name = "last_accessed")
  var lastAccessed: LocalDateTime = LocalDateTime.now(),

  @Column
  var created: LocalDateTime = LocalDateTime.now(),

  @Column(name = "web_server_redirect_uri")
  @Convert(converter = StringListConverter::class)
  val webServerRedirectUri: List<String> = emptyList(),

  @Column(name = "secret_updated")
  var secretUpdated: LocalDateTime = LocalDateTime.now()
) {
  fun resetLastAccessed() {
    lastAccessed = LocalDateTime.now()
  }

  val authoritiesWithoutPrefix: List<String>
    get() = authorities.map { it.substringAfter("ROLE_") }

  val mfa: MfaAccess
    get() = MfaAccess.valueOf(additionalInformation["mfa"] as? String ?: "none")

  val baseClientId: String
    get() = id.replace(regex = "-[0-9]*$".toRegex(), replacement = "")
}

@Converter
class JsonToMapConverter : AttributeConverter<Map<String, Any>, String?> {
  private val objectMapper: ObjectMapper = ObjectMapper()

  override fun convertToDatabaseColumn(customerInfo: Map<String, Any>): String? = try {
    objectMapper.writeValueAsString(customerInfo)
  } catch (e: JsonProcessingException) {
    log.error("JSON writing error", e)
    null
  }

  @Suppress("JpaAttributeTypeInspection", "UNCHECKED_CAST")
  override fun convertToEntityAttribute(customerInfoJSON: String?): Map<String, Any> =
    if (customerInfoJSON.isNullOrEmpty()) emptyMap()
    else try {
      objectMapper.readValue(customerInfoJSON, Map::class.java)
    } catch (e: IOException) {
      @Suppress("JpaAttributeTypeInspection")
      log.error("JSON reading error", e)
      emptyMap<String, Any>()
    } as Map<String, Any>

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Converter
class StringListConverter : AttributeConverter<List<String>, String?> {
  override fun convertToDatabaseColumn(stringList: List<String>): String =
    stringList.filter { it.isNotEmpty() }.joinToString(",") { it.trim() }

  override fun convertToEntityAttribute(string: String?): List<String> =
    string?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}

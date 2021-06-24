package uk.gov.justice.digital.hmpps.oauth2server.security

import com.fasterxml.jackson.annotation.JsonValue

enum class AuthSource {
  auth, azuread, delius, nomis, none;

  @JsonValue
  val source: String = name

  companion object {
    @JvmStatic
    fun fromNullableString(source: String?): AuthSource {
      return source?.let { valueOf(source.lowercase()) } ?: none
    }
  }
}

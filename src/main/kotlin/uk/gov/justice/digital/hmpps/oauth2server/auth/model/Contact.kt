package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
data class Contact(
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val type: ContactType,
) {
  constructor(type: ContactType, value: String, verified: Boolean = false) : this(type) {
    this.value = value
    this.verified = verified
  }

  var value: String? = null
  var verified: Boolean = false
}

enum class ContactType(val description: String) {
  SECONDARY_EMAIL("Secondary email"), MOBILE_PHONE("Mobile phone")
}

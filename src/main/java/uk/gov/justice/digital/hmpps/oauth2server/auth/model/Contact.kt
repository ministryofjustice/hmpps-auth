package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
data class Contact(@Enumerated(EnumType.STRING) var type: ContactType, var value: String)

enum class ContactType {
  EMAIL
}

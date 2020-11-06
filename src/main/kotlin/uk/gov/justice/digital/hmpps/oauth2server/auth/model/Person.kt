package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class Person(
  @Column(name = "first_name", nullable = false) var firstName: String,
  @Column(name = "last_name", nullable = false) var lastName: String,
) {

  val name: String
    get() = "$firstName $lastName"
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import java.time.LocalDateTime
import java.util.UUID

class Helper

fun buildUser(): User {
  return User.builder()
    .id(UUID.randomUUID())
    .username("firstlast")
    .email("first.last@.justice.go.uk")
    .verified(true)
    .locked(false)
    .enabled(true)
    .source(AuthSource.auth)
    .passwordExpiry(LocalDateTime.now().plusHours(1L))
    .lastLoggedIn(LocalDateTime.now().minusHours(1L))
    .mobile("07700 900322")
    .mobileVerified(true)
    .mfaPreference(User.MfaPreferenceType.EMAIL)
    .person(Person("first", "last"))
    .build()
}

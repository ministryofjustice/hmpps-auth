package uk.gov.justice.digital.hmpps.oauth2server.utils

fun String.sanitise(): String {
  return this
    .replace("\r", "")
    .replace("\n", "")
}

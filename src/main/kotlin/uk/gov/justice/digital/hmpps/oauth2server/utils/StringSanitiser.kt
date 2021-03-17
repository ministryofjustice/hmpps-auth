package uk.gov.justice.digital.hmpps.oauth2server.utils

fun String.removeAllCrLf() =
  replace("\r", "")
    .replace("\n", "")

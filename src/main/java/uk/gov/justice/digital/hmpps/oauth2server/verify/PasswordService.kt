package uk.gov.justice.digital.hmpps.oauth2server.verify

interface PasswordService {
  fun setPassword(token: String, password: String?)
}

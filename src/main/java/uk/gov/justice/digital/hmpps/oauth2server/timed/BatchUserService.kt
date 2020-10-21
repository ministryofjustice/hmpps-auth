package uk.gov.justice.digital.hmpps.oauth2server.timed

interface BatchUserService {
  fun processInBatches(): Int
}

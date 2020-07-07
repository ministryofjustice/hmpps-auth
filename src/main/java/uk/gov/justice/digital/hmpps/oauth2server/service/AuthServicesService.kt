package uk.gov.justice.digital.hmpps.oauth2server.service

import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository

@org.springframework.stereotype.Service
class AuthServicesService(private val oauthServiceRepository: OauthServiceRepository) {
  fun list(): MutableIterable<Service> = oauthServiceRepository.findAllByOrderByName()
}

package uk.gov.justice.digital.hmpps.oauth2server.landing

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository

@Transactional(transactionManager = "authTransactionManager", readOnly = true)
@Service
class LandingService(private val oauthServiceRepository: OauthServiceRepository) {
  fun findAllServices(): List<uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service> =
    oauthServiceRepository.findAllByEnabledTrueOrderByName()
}

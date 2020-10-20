package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service

interface OauthServiceRepository : CrudRepository<Service, String> {
  fun findAllByEnabledTrueOrderByName(): List<Service>
  fun findAllByOrderByName(): List<Service>
}

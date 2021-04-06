package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Client

interface ClientRepository : CrudRepository<Client, String> {
  fun findByIdStartsWithOrderById(clientId: String): List<Client>
}

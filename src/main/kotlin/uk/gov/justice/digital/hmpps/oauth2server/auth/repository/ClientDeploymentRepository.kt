package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ClientDeployment

interface ClientDeploymentRepository : CrudRepository<ClientDeployment, String> {

  fun deleteByBaseClientId(baseClientId: String)
}

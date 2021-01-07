package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ChildGroup

interface ChildGroupRepository : CrudRepository<ChildGroup, String> {

  fun findByGroupCode(groupCode: String?): ChildGroup?

  fun deleteByGroupCode(groupCode: String?)
}

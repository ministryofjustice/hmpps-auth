package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.lang.NonNull
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group
import java.util.Optional

interface GroupRepository : CrudRepository<Group, String> {
  @NonNull
  fun findAllByOrderByGroupName(): List<Group>
  fun findByGroupCode(groupCode: String?): Optional<Group>
}

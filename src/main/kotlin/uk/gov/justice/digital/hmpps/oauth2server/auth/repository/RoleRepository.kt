package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.NonNull
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority
import java.util.Optional
import java.util.UUID

interface RoleRepository : CrudRepository<Authority, String> {
  @NonNull
  fun findAllByOrderByRoleName(): List<Authority>
  fun findByRoleCode(roleCode: String?): Optional<Authority>

  @Query("select distinct r from User u join u.groups g join g.assignableRoles gar join gar.role r where u.username = ?1 order by r.roleName")
  fun findByGroupAssignableRolesForUsername(username: String?): Set<Authority>

  @Query("select distinct r from User u join u.groups g join g.assignableRoles gar join gar.role r where u.id = ?1 order by r.roleName")
  fun findByGroupAssignableRolesForUserId(userId: UUID?): Set<Authority>
}

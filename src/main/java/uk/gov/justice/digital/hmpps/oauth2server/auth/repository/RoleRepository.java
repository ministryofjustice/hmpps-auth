package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends CrudRepository<Authority, String> {
    @NonNull
    List<Authority> findAllByOrderByRoleName();

    Optional<Authority> findByRoleCode(String roleCode);

    @Query("select distinct r from User u join u.groups g join g.assignableRoles gar join gar.role r where u.username = ?1 order by r.roleName")
    Set<Authority> findByGroupAssignableRolesForUsername(String username);
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends CrudRepository<Authority, String> {
    @NonNull
    List<Authority> findAllByOrderByRoleName();

    Optional<Authority> findByAuthority(String roleCode);
}

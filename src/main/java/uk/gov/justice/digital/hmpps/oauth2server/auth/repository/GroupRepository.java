package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends CrudRepository<Group, String> {
    @NonNull
    List<Group> findAllByOrderByGroupName();

    Optional<Group> findByGroupCode(String groupCode);
}

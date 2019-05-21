package uk.gov.justice.digital.hmpps.oauth2server.oasys.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.digital.hmpps.oauth2server.oasys.model.OasysUser;

@Repository
public interface OasysUserRepository extends CrudRepository<OasysUser, String> {
}

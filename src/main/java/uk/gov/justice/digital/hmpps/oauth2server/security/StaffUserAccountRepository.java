package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.model.StaffUserAccount;

public interface StaffUserAccountRepository extends CrudRepository<StaffUserAccount, String> {

}

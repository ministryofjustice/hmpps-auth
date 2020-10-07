package uk.gov.justice.digital.hmpps.oauth2server.nomis.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import java.util.List;

public interface StaffUserAccountRepository extends CrudRepository<NomisUserPersonDetails, String> {
    List<NomisUserPersonDetails> findByStaffFirstNameIgnoreCaseAndStaffLastNameIgnoreCase(String firstName, String lastName);
}

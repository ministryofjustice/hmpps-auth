package uk.gov.justice.digital.hmpps.oauth2server.nomis.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffIdentifier;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffIdentifierIdentity;

public interface StaffIdentifierRepository extends CrudRepository<StaffIdentifier, StaffIdentifierIdentity> {

    StaffIdentifier findById_TypeAndId_IdentificationNumber(String type, String identificationNumber);

}

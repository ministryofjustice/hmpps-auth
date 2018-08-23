package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.model.StaffIdentifier;
import uk.gov.justice.digital.hmpps.oauth2server.model.StaffIdentifierIdentity;

public interface StaffIdentifierRepository extends CrudRepository<StaffIdentifier, StaffIdentifierIdentity> {

    StaffIdentifier findById_TypeAndId_IdentificationNumber(String type, String identificationNumber);

}

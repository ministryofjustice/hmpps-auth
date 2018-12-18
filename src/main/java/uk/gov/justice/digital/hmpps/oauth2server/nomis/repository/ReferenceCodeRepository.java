package uk.gov.justice.digital.hmpps.oauth2server.nomis.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.DomainCodeIdentifier;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceCode;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceDomain;

import java.util.List;

public interface ReferenceCodeRepository extends CrudRepository<ReferenceCode, DomainCodeIdentifier> {

    List<ReferenceCode> findByDomainCodeIdentifierDomainAndActiveIsTrueAndExpiredDateIsNull(ReferenceDomain domain);
}

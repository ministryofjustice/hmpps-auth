package uk.gov.justice.digital.hmpps.oauth2server.auth.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service;

import java.util.List;

public interface OauthServiceRepository extends CrudRepository<Service, String> {
    List<Service> findAllByEnabledTrueOrderByName();

    List<Service> findAllByOrderByName();
}

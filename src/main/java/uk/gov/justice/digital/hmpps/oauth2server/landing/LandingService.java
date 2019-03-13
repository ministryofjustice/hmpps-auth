package uk.gov.justice.digital.hmpps.oauth2server.landing;

import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Service;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.OauthServiceRepository;

import java.util.List;

@Transactional(readOnly = true)
@org.springframework.stereotype.Service
public class LandingService {
    private final OauthServiceRepository oauthServiceRepository;

    public LandingService(final OauthServiceRepository oauthServiceRepository) {
        this.oauthServiceRepository = oauthServiceRepository;
    }

    public List<Service> findAllServices() {
        return oauthServiceRepository.findAllByEnabledTrueOrderByName();
    }
}

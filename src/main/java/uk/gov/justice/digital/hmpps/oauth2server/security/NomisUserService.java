package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
@AllArgsConstructor
public class NomisUserService {
    private final StaffUserAccountRepository staffUserAccountRepository;

    public Optional<StaffUserAccount> getNomisUserByUsername(final String username) {
        return staffUserAccountRepository.findById(StringUtils.upperCase(username));
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
@AllArgsConstructor
public abstract class NomisUserService {
    private final StaffUserAccountRepository staffUserAccountRepository;

    public Optional<NomisUserPersonDetails> getNomisUserByUsername(final String username) {
        return staffUserAccountRepository.findById(StringUtils.upperCase(username));
    }

    public abstract void changePassword(String username, String password);

    public abstract void changePasswordWithUnlock(String username, String password);

    public abstract void lockAccount(String username);

    public List<NomisUserPersonDetails> findPrisonUsersByFirstAndLastNames(final String firstName, final String lastName) {
        return staffUserAccountRepository.findByStaffFirstNameIgnoreCaseAndStaffLastNameIgnoreCase(firstName, lastName);
    }
}

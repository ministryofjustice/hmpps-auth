package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final StaffUserAccountRepository userRepository;
    private final StaffIdentifierRepository staffIdentifierRepository;
    private final UserEmailRepository userEmailRepository;

    public UserService(final StaffUserAccountRepository userRepository, final StaffIdentifierRepository staffIdentifierRepository, final UserEmailRepository userEmailRepository) {
        this.userRepository = userRepository;
        this.staffIdentifierRepository = staffIdentifierRepository;
        this.userEmailRepository = userEmailRepository;
    }

    public Optional<StaffUserAccount> getUserByUsername(final String username) {
        return userRepository.findById(StringUtils.upperCase(username));
    }

    public StaffUserAccount getUserByExternalIdentifier(final String idType, final String id, final boolean activeOnly) {
        final var staffIdentifier = staffIdentifierRepository.findById_TypeAndId_IdentificationNumber(idType, id);
        Optional<StaffUserAccount> userDetail = Optional.empty();
        if (staffIdentifier != null) {
            final var staff = staffIdentifier.getStaff();

            if (activeOnly && !staff.isActive()) {
                log.info("Staff member found for external identifier with idType [{}] and id [{}] but not active.", idType, id);
            } else {
                userDetail = Optional.ofNullable(staff.getAccountByType("GENERAL"));
            }
        }

        return userDetail.orElseThrow(() ->
                new EntityNotFoundException(String.format("User not found for external identifier with idType [%s] and id [%s].", idType, id)));
    }

    public Optional<UserEmail> getAuthUserByUsername(final String username) {
        return userEmailRepository.findByUsernameAndMasterIsTrue(StringUtils.upperCase(username));
    }

    public Optional<UserPersonDetails> findUser(final String username) {
        return getAuthUserByUsername(username).map(UserPersonDetails.class::cast).
                or(() -> getUserByUsername(username).map(UserPersonDetails.class::cast));
    }
}

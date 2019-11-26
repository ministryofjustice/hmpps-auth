package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    private final NomisUserService nomisUserService;
    private final AuthUserService authUserService;
    private final StaffIdentifierRepository staffIdentifierRepository;
    private final UserRepository userRepository;

    public UserService(final NomisUserService nomisUserService, final AuthUserService authUserService, final StaffIdentifierRepository staffIdentifierRepository,
                       final UserRepository userRepository) {
        this.nomisUserService = nomisUserService;
        this.authUserService = authUserService;
        this.staffIdentifierRepository = staffIdentifierRepository;
        this.userRepository = userRepository;
    }

    StaffUserAccount getUserByExternalIdentifier(final String idType, final String id, final boolean activeOnly) {
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

    public Optional<UserPersonDetails> findMasterUserPersonDetails(final String username) {
        return authUserService.getAuthUserByUsername(username).map(UserPersonDetails.class::cast).
                or(() -> nomisUserService.getNomisUserByUsername(username).map(UserPersonDetails.class::cast));
    }

    public Optional<User> findUser(final String username) {
        return userRepository.findByUsername(StringUtils.upperCase(username));
    }
}

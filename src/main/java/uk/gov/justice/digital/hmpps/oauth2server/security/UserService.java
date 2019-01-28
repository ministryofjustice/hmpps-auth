package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffIdentifier;
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

    public UserService(StaffUserAccountRepository userRepository, StaffIdentifierRepository staffIdentifierRepository) {
        this.userRepository = userRepository;
        this.staffIdentifierRepository = staffIdentifierRepository;
    }

    public Optional<StaffUserAccount> getUserByUsername(String username) {
        return userRepository.findById(StringUtils.upperCase(username));
    }

    public StaffUserAccount getUserByExternalIdentifier(String idType, String id, boolean activeOnly) {
        StaffIdentifier staffIdentifier = staffIdentifierRepository.findById_TypeAndId_IdentificationNumber(idType, id);
        Optional<StaffUserAccount> userDetail = Optional.empty();
        if (staffIdentifier != null) {
            Staff staff = staffIdentifier.getStaff();

            if (activeOnly && !staff.isActive()) {
                log.info("Staff member found for external identifier with idType [{}] and id [{}] but not active.", idType, id);
            } else {
                userDetail = Optional.ofNullable(staff.getAccountByType("GENERAL"));
            }
        }

        return userDetail.orElseThrow(() ->
                new EntityNotFoundException(String.format("User not found for external identifier with idType [%s] and id [%s].", idType, id)));
    }

}

package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final StaffUserAccountRepository userRepository;
    private final StaffIdentifierRepository staffIdentifierRepository;
    private final UserEmailRepository userEmailRepository;
    private final TelemetryClient telemetryClient;

    public UserService(final StaffUserAccountRepository userRepository,
                       final StaffIdentifierRepository staffIdentifierRepository,
                       final UserEmailRepository userEmailRepository,
                       final TelemetryClient telemetryClient) {
        this.userRepository = userRepository;
        this.staffIdentifierRepository = staffIdentifierRepository;
        this.userEmailRepository = userEmailRepository;
        this.telemetryClient = telemetryClient;
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
        return userEmailRepository.findByUsernameAndMasterIsTrue(StringUtils.upperCase(StringUtils.trim(username)));
    }

    public Optional<UserPersonDetails> findUser(final String username) {
        return getAuthUserByUsername(username).map(UserPersonDetails.class::cast).
                or(() -> getUserByUsername(username).map(UserPersonDetails.class::cast));
    }

    public List<UserEmail> findAuthUsersByEmail(final String email) {
        return userEmailRepository.findByEmailAndMasterIsTrueOrderByUsername(StringUtils.lowerCase(StringUtils.trim(email)));
    }

    @Transactional
    public void enableUser(final String username, final String admin) {
        changeUserEnabled(username, true, admin);
    }

    @Transactional
    public void disableUser(final String username, final String admin) {
        changeUserEnabled(username, false, admin);
    }

    private void changeUserEnabled(final String username, final boolean enabled, final String admin) {
        final var userEmail = getAuthUserByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", username)));
        userEmail.setEnabled(enabled);
        userEmailRepository.save(userEmail);
        telemetryClient.trackEvent("AuthUserChangeEnabled",
                Map.of("username", username, "enabled", Boolean.toString(enabled), "admin", admin), null);
    }
}

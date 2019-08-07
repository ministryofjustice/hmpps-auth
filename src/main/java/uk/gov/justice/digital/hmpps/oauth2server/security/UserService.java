package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import javax.persistence.EntityNotFoundException;
import java.util.Collection;
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
    private final MaintainUserCheck maintainUserCheck;

    public UserService(final StaffUserAccountRepository userRepository,
                       final StaffIdentifierRepository staffIdentifierRepository,
                       final UserEmailRepository userEmailRepository,
                       final TelemetryClient telemetryClient,
                       final MaintainUserCheck maintainUserCheck) {
        this.userRepository = userRepository;
        this.staffIdentifierRepository = staffIdentifierRepository;
        this.userEmailRepository = userEmailRepository;
        this.telemetryClient = telemetryClient;
        this.maintainUserCheck = maintainUserCheck;
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

    @Transactional(transactionManager = "authTransactionManager")
    public void enableUser(final String usernameInDb, final String admin, final Collection<? extends GrantedAuthority> authorities) throws MaintainUserCheck.AuthUserGroupRelationshipException {

        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(usernameInDb)
                .orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", usernameInDb)));

        maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, userEmail);

        changeUserEnabled(userEmail, true, admin);
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void disableUser(final String usernameInDb, final String admin, final Collection<? extends GrantedAuthority> authorities) throws MaintainUserCheck.AuthUserGroupRelationshipException {

        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(usernameInDb)
                .orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", usernameInDb)));

        maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, userEmail);

        changeUserEnabled(userEmail, false, admin);

    }

    private void changeUserEnabled(final UserEmail userEmail, final boolean enabled, final String admin) {
        userEmail.setEnabled(enabled);
        userEmailRepository.save(userEmail);
        telemetryClient.trackEvent("AuthUserChangeEnabled",
                Map.of("username", userEmail.getUsername(), "enabled", Boolean.toString(enabled), "admin", admin), null);
    }

    public Optional<UserEmail> findUserEmail(final String username) {
        return userEmailRepository.findById(StringUtils.upperCase(username));
    }
}

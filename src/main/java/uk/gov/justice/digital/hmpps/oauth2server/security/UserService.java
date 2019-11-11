package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;
import uk.gov.justice.digital.hmpps.oauth2server.security.MaintainUserCheck.AuthUserGroupRelationshipException;
import uk.gov.justice.digital.hmpps.oauth2server.utils.EmailHelper;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final StaffUserAccountRepository staffUserAccountRepository;
    private final StaffIdentifierRepository staffIdentifierRepository;
    private final UserRepository userRepository;
    private final TelemetryClient telemetryClient;
    private final MaintainUserCheck maintainUserCheck;
    private final int disableAgeTrigger;

    public UserService(final StaffUserAccountRepository staffUserAccountRepository,
                       final StaffIdentifierRepository staffIdentifierRepository,
                       final UserRepository userRepository,
                       final TelemetryClient telemetryClient,
                       final MaintainUserCheck maintainUserCheck,
                       @Value("${application.authentication.disable.age-trigger}") final int disableAgeTrigger) {
        this.staffUserAccountRepository = staffUserAccountRepository;
        this.staffIdentifierRepository = staffIdentifierRepository;
        this.userRepository = userRepository;
        this.telemetryClient = telemetryClient;
        this.maintainUserCheck = maintainUserCheck;
        this.disableAgeTrigger = disableAgeTrigger;
    }

    private Optional<StaffUserAccount> getUserByUsername(final String username) {
        return staffUserAccountRepository.findById(StringUtils.upperCase(username));
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

    public Optional<User> getAuthUserByUsername(final String username) {
        return userRepository.findByUsernameAndMasterIsTrue(StringUtils.upperCase(StringUtils.trim(username)));
    }

    public Optional<UserPersonDetails> findUser(final String username) {
        return getAuthUserByUsername(username).map(UserPersonDetails.class::cast).
                or(() -> getUserByUsername(username).map(UserPersonDetails.class::cast));
    }

    public List<User> findAuthUsersByEmail(final String email) {
        return userRepository.findByEmailAndMasterIsTrueOrderByUsername(EmailHelper.format(email));
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void enableUser(final String usernameInDb, final String admin, final Collection<? extends GrantedAuthority> authorities) throws AuthUserGroupRelationshipException {
        changeUserEnabled(usernameInDb, true, admin, authorities);
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void disableUser(final String usernameInDb, final String admin, final Collection<? extends GrantedAuthority> authorities) throws AuthUserGroupRelationshipException {
        changeUserEnabled(usernameInDb, false, admin, authorities);
    }

    private void changeUserEnabled(final String username, final boolean enabled, final String admin, final Collection<? extends GrantedAuthority> authorities) throws AuthUserGroupRelationshipException {
        final var user = userRepository.findByUsernameAndMasterIsTrue(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("User not found with username %s", username)));

        maintainUserCheck.ensureUserLoggedInUserRelationship(admin, authorities, user);

        user.setEnabled(enabled);

        // give user 7 days grace if last logged in more than x days ago
        if (user.getLastLoggedIn().isBefore(LocalDateTime.now().minusDays(disableAgeTrigger))) {
            user.setLastLoggedIn(LocalDateTime.now().minusDays(disableAgeTrigger - 7));
        }
        userRepository.save(user);
        telemetryClient.trackEvent("AuthUserChangeEnabled",
                Map.of("username", user.getUsername(), "enabled", Boolean.toString(enabled), "admin", admin), null);
    }

    public Optional<User> findAuthUser(final String username) {
        return userRepository.findByUsername(StringUtils.upperCase(username));
    }
}

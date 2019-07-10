package uk.gov.justice.digital.hmpps.oauth2server.maintain;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.GroupRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


@Service
@Slf4j
@AllArgsConstructor
@Transactional(transactionManager = "authTransactionManager", readOnly = true)
public class AuthUserGroupService {
    private final UserEmailRepository userEmailRepository;
    private final GroupRepository groupRepository;
    private final TelemetryClient telemetryClient;

    @Transactional(transactionManager = "authTransactionManager")
    public void addGroup(final String username, final String groupCode, final String modifier) throws AuthUserGroupException {
        // already checked that user exists
        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username).orElseThrow();

        final var groupFormatted = formatGroup(groupCode);

        // check that group exists
        final var group = groupRepository.findByGroupCode(groupFormatted).orElseThrow(() -> new AuthUserGroupException("group", "notfound"));

        if (userEmail.getGroups().contains(group)) {
            throw new AuthUserGroupExistsException();
        }

        // TODO: Validate that the group is allowed to be added to the user:
        // 1. If super user then can add anyone to anything
        // 2. If group admin then needs to be one of their groups and user can't be a member of a different group

        log.info("Adding group {} to user {}", groupFormatted, username);
        userEmail.getGroups().add(group);
        telemetryClient.trackEvent("AuthUserGroupAddSuccess", Map.of("username", username, "group", groupFormatted, "admin", modifier), null);
        userEmailRepository.save(userEmail);
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void removeGroup(final String username, final String groupCode, final String modifier) throws AuthUserGroupException {
        final var groupFormatted = formatGroup(groupCode);
        // already checked that user exists
        final var userEmail = userEmailRepository.findByUsernameAndMasterIsTrue(username).orElseThrow();

        if (userEmail.getGroups().stream().map(Group::getGroupCode).noneMatch(a -> a.equals(groupFormatted))) {
            throw new AuthUserGroupException("group", "missing");
        }

        log.info("Removing group {} from user {}", groupFormatted, username);
        userEmail.getGroups().removeIf(a -> a.getGroupCode().equals(groupFormatted));

        telemetryClient.trackEvent("AuthUserGroupRemoveSuccess", Map.of("username", username, "group", groupCode, "admin", modifier), null);
        userEmailRepository.save(userEmail);
    }

    private String formatGroup(final String group) {
        return StringUtils.upperCase(StringUtils.trim(group));
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Optional<Set<Group>> getAuthUserGroups(final String username) {
        final var user = userEmailRepository.findByUsernameAndMasterIsTrue(StringUtils.upperCase(StringUtils.trim(username)));
        return user.map(u -> {
            Hibernate.initialize(u.getGroups());
            return u.getGroups();
        });
    }

    public static class AuthUserGroupExistsException extends AuthUserGroupException {
        public AuthUserGroupExistsException() {
            super("group", "exists");
        }
    }


    @Getter
    public static class AuthUserGroupException extends Exception {
        private final String errorCode;
        private final String field;

        public AuthUserGroupException(final String field, final String errorCode) {
            super(String.format("Add group failed for field %s with reason: %s", field, errorCode));

            this.field = field;
            this.errorCode = errorCode;
        }
    }
}

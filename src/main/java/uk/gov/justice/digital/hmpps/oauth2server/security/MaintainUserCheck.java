package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.google.common.collect.Sets;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;

import java.util.Collection;

@Service
public class MaintainUserCheck {

    private final UserEmailRepository userEmailRepository;

    public MaintainUserCheck(
            final UserEmailRepository userEmailRepository) {
        this.userEmailRepository = userEmailRepository;
    }

    public static boolean canMaintainAuthUsers(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_MAINTAIN_OAUTH_USERS"::equals);
    }

    public void ensureUserLoggedInUserRelationship(final String loggedInUser, final Collection<? extends GrantedAuthority> authorities, final UserEmail userEmail) throws AuthUserGroupRelationshipException {
        // if they have maintain privileges then all good
        if (MaintainUserCheck.canMaintainAuthUsers(authorities)) {
            return;
        }
        // otherwise group managers must have a group in common for maintenance
        final var loggedInUserEmail = userEmailRepository.findByUsernameAndMasterIsTrue(loggedInUser).orElseThrow();
        if (Sets.intersection(loggedInUserEmail.getGroups(), userEmail.getGroups()).isEmpty()) {
            // no group in common, so disallow
            throw new MaintainUserCheck.AuthUserGroupRelationshipException(userEmail.getName(), "User not with your groups");
        }
    }

    @Getter
    public static class AuthUserGroupRelationshipException extends Exception {
        private final String username;
        private final String errorCode;

        public AuthUserGroupRelationshipException(final String username, final String errorCode) {
            super(String.format("Unable to maintain user: %s with reason: %s", username, errorCode));

            this.username = username;
            this.errorCode = errorCode;
        }
    }
}

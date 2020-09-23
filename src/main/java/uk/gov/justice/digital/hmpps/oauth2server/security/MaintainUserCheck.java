package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.google.common.collect.Sets;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;

import java.util.Collection;

@Service
public class MaintainUserCheck {

    private final UserRepository userRepository;

    public MaintainUserCheck(
            final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static boolean canMaintainAuthUsers(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_MAINTAIN_OAUTH_USERS"::equals);
    }

    public void ensureUserLoggedInUserRelationship(final String loggedInUser, final Collection<? extends GrantedAuthority> authorities, final User user) throws AuthUserGroupRelationshipException {
        // if they have maintain privileges then all good
        if (canMaintainAuthUsers(authorities)) {
            return;
        }
        // otherwise group managers must have a group in common for maintenance
        final var loggedInUserEmail = userRepository.findByUsernameAndMasterIsTrue(loggedInUser).orElseThrow();
        if (Sets.intersection(loggedInUserEmail.getGroups(), user.getGroups()).isEmpty()) {
            // no group in common, so disallow
            throw new AuthUserGroupRelationshipException(user.getName(), "User not with your groups");
        }
    }

    public static class AuthUserGroupRelationshipException extends Exception {
        private final String username;
        private final String errorCode;

        public AuthUserGroupRelationshipException(final String username, final String errorCode) {
            super(String.format("Unable to maintain user: %s with reason: %s", username, errorCode));

            this.username = username;
            this.errorCode = errorCode;
        }

        public String getUsername() {
            return this.username;
        }

        public String getErrorCode() {
            return this.errorCode;
        }
    }
}

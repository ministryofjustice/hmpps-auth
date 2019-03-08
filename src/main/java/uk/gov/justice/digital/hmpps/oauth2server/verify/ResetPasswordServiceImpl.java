package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.weddini.throttling.Throttling;
import com.weddini.throttling.ThrottlingType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.AlterUserService;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Transactional
public class ResetPasswordServiceImpl extends PasswordServiceImpl implements ResetPasswordService {

    private final UserEmailRepository userEmailRepository;
    private final UserTokenRepository userTokenRepository;
    private final UserService userService;
    private final AlterUserService alterUserService;
    private final NotificationClientApi notificationClient;
    private final String resetTemplateId;
    private final String resetUnavailableTemplateId;
    private final String resetUnavailableByEmailTemplateId;
    private final String resetUnavailableEmailNotFoundTemplateId;

    public ResetPasswordServiceImpl(final UserEmailRepository userEmailRepository,
                                    final UserTokenRepository userTokenRepository, final UserService userService,
                                    final AlterUserService alterUserService,
                                    final NotificationClientApi notificationClient,
                                    @Value("${application.notify.reset.template}") final String resetTemplateId,
                                    @Value("${application.notify.reset-unavailable.template}") final String resetUnavailableTemplateId,
                                    @Value("${application.notify.reset-unavailable-by-email.template}") final String resetUnavailableByEmailTemplateId,
                                    @Value("${application.notify.reset-unavailable-email-not-found.template}") final String resetUnavailableEmailNotFoundTemplateId,
                                    final PasswordEncoder passwordEncoder,
                                    @Value("${application.authentication.password-age}") final long passwordAge) {
        super(passwordEncoder, passwordAge);
        this.userEmailRepository = userEmailRepository;
        this.userTokenRepository = userTokenRepository;
        this.userService = userService;
        this.alterUserService = alterUserService;
        this.notificationClient = notificationClient;
        this.resetTemplateId = resetTemplateId;
        this.resetUnavailableTemplateId = resetUnavailableTemplateId;
        this.resetUnavailableByEmailTemplateId = resetUnavailableByEmailTemplateId;
        this.resetUnavailableEmailNotFoundTemplateId = resetUnavailableEmailNotFoundTemplateId;
    }

    @Override
    @Throttling(type = ThrottlingType.SpEL, expression = "T(uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper).retrieveIpFromRequest()", limit = 6, timeUnit = TimeUnit.MINUTES)
    public Optional<String> requestResetPassword(final String usernameOrEmailAddress, final String url) throws NotificationClientRuntimeException {
        final Optional<UserEmail> optionalUserEmail;
        final boolean foundMultipleMatches;
        if (StringUtils.contains(usernameOrEmailAddress, "@")) {
            final var matches = userEmailRepository.findByEmail(usernameOrEmailAddress.toLowerCase());
            if (matches.isEmpty()) {
                // no match, but got an email address so let them know
                sendEmail(usernameOrEmailAddress, resetUnavailableEmailNotFoundTemplateId, Collections.emptyMap(), usernameOrEmailAddress);
                return Optional.empty();
            }

            foundMultipleMatches = matches.size() > 1;
            optionalUserEmail = Optional.of(matches.get(0));
        } else {
            foundMultipleMatches = false;
            optionalUserEmail = userEmailRepository.findById(usernameOrEmailAddress.toUpperCase());
        }

        if (optionalUserEmail.isEmpty() || StringUtils.isEmpty(optionalUserEmail.get().getEmail())) {
            // no user found or email address found, so nothing more we can do.  Bail
            return Optional.empty();
        }

        final var userEmail = optionalUserEmail.get();
        final var templateAndParameters = getTemplateAndParameters(url, foundMultipleMatches, userEmail);

        sendEmail(userEmail.getUsername(), templateAndParameters, userEmail.getEmail());

        return Optional.ofNullable(templateAndParameters.getResetLink());
    }

    private TemplateAndParameters getTemplateAndParameters(final String url, final boolean foundMultipleMatches, final UserEmail userEmail) {
        final UserPersonDetails userDetails;
        if (userEmail.isMaster()) {
            userDetails = userEmail;
        } else {
            final var userOptional = userService.findUser(userEmail.getUsername());
            if (userOptional.isEmpty()) {
                // shouldn't really happen, means that a nomis user exists in auth but not in nomis
                return new TemplateAndParameters(resetUnavailableTemplateId, userEmail.getUsername());
            }
            userDetails = userOptional.get();
        }
        // only allow reset for active accounts that aren't locked
        // or are locked by getting password incorrect (in either c-nomis or auth)
        final var firstName = userDetails.getFirstName();
        if (foundMultipleMatches) {
            // for now, tell user they can't reset by email address as more than one match found
            return new TemplateAndParameters(resetUnavailableByEmailTemplateId, firstName);
        }
        if (passwordAllowedToBeReset(userEmail, userDetails)) {
            final var userTokenOptional = userTokenRepository.findByTokenTypeAndUserEmail(TokenType.RESET, userEmail);
            // delete any existing token
            userTokenOptional.ifPresent(userTokenRepository::delete);

            final var userToken = new UserToken(TokenType.RESET, userEmail);
            userTokenRepository.save(userToken);

            final var resetLink = url + userToken.getToken();
            return new TemplateAndParameters(resetTemplateId, firstName, resetLink);
        }
        return new TemplateAndParameters(resetUnavailableTemplateId, firstName);
    }

    private void sendEmail(final String username, final TemplateAndParameters templateAndParameters, final String email) throws NotificationClientRuntimeException {
        sendEmail(username, templateAndParameters.getTemplate(), templateAndParameters.getParameters(), email);
    }

    private void sendEmail(final String username, final String template, final Map<String, String> parameters, final String email) throws NotificationClientRuntimeException {
        try {
            log.info("Sending reset password to notify for user {}", username);
            notificationClient.sendEmail(template, email, parameters, null);
        } catch (final NotificationClientException e) {
            log.warn("Failed to send reset password to notify for user {}", username, e);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                try {
                    notificationClient.sendEmail(template, email, parameters, null);
                } catch (final NotificationClientException e1) {
                    throw new NotificationClientRuntimeException(e1);
                }
            }
            throw new NotificationClientRuntimeException(e);
        }
    }

    private boolean passwordAllowedToBeReset(final UserEmail userEmail, final UserPersonDetails userPersonDetails) {
        if (userEmail.isMaster()) {
            // for auth users they must be enabled (so can be locked)
            return userEmail.isEnabled();
        }
        // otherwise must be nomis user so will have a staff account instead.
        final var staffUserAccount = (StaffUserAccount) userPersonDetails;
        final var status = staffUserAccount.getAccountDetail().getStatus();
        return staffUserAccount.getStaff().isActive() && (!status.isLocked() || status.isUserLocked() || userEmail.isLocked());
    }

    @Override
    public void setPassword(final String token, final String password) {
        final var userToken = userTokenRepository.findById(token).orElseThrow();
        final var userEmail = userToken.getUserEmail();
        final var userOptional = userEmail.isMaster() ? Optional.of(userEmail) : userService.findUser(userEmail.getUsername());

        // before we reset, ensure user allowed to still reset password
        if (userOptional.map(user -> !passwordAllowedToBeReset(userEmail, user)).orElse(Boolean.TRUE)) {
            // failed, so let user know
            throw new LockedException("locked");
        }

        userEmail.setVerified(true);
        userEmail.setLocked(false);

        // if we're the master of this user record deal with the change of password here
        if (userEmail.isMaster()) {
            changeAuthPassword(userEmail, password);
        } else {
            alterUserService.changePasswordWithUnlock(userEmail.getUsername(), password);
        }

        userEmailRepository.save(userEmail);
        userTokenRepository.delete(userToken);
    }

    @AllArgsConstructor
    @Getter
    private static class TemplateAndParameters {
        private final String template;
        private final String firstName;
        private final String resetLink;

        TemplateAndParameters(final String template, final String firstName) {
            this(template, firstName, null);
        }

        Map<String, String> getParameters() {
            if (resetLink == null) {
                return Map.of("firstName", firstName);
            }
            return Map.of("firstName", firstName, "resetLink", resetLink);
        }
    }

    // Throttling doesn't allow checked exceptions to be thrown, hence wrapped in a runtime exception
    public static class NotificationClientRuntimeException extends RuntimeException {

        public NotificationClientRuntimeException(final NotificationClientException e) {
            super(e);
        }
    }
}

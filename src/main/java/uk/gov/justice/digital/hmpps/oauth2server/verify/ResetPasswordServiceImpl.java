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
    private final String resetUnavailableEmailNotFoundTemplateId;
    private final String resetPasswordConfirmedTemplateId;

    public ResetPasswordServiceImpl(final UserEmailRepository userEmailRepository,
                                    final UserTokenRepository userTokenRepository, final UserService userService,
                                    final AlterUserService alterUserService,
                                    final NotificationClientApi notificationClient,
                                    @Value("${application.notify.reset.template}") final String resetTemplateId,
                                    @Value("${application.notify.reset-unavailable.template}") final String resetUnavailableTemplateId,
                                    @Value("${application.notify.reset-unavailable-email-not-found.template}") final String resetUnavailableEmailNotFoundTemplateId,
                                    @Value("${application.notify.reset-password.template}") String resetPasswordConfirmedTemplateId,
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
        this.resetUnavailableEmailNotFoundTemplateId = resetUnavailableEmailNotFoundTemplateId;
        this.resetPasswordConfirmedTemplateId = resetPasswordConfirmedTemplateId;
    }

    @Override
    @Throttling(type = ThrottlingType.SpEL, expression = "T(uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper).retrieveIpFromRequest()", limit = 6, timeUnit = TimeUnit.MINUTES)
    public Optional<String> requestResetPassword(final String usernameOrEmailAddress, final String url) throws NotificationClientRuntimeException {
        final Optional<UserEmail> optionalUserEmail;
        final boolean multipleMatchesAndCanBeReset;
        if (StringUtils.contains(usernameOrEmailAddress, "@")) {
            final var matches = userEmailRepository.findByEmail(usernameOrEmailAddress.toLowerCase());
            if (matches.isEmpty()) {
                // no match, but got an email address so let them know
                sendEmail(usernameOrEmailAddress, resetUnavailableEmailNotFoundTemplateId, Collections.emptyMap(), usernameOrEmailAddress.toLowerCase());
                return Optional.empty();
            }

            final var passwordCanBeReset = matches.stream().filter(this::passwordAllowedToBeReset).findFirst();
            multipleMatchesAndCanBeReset = passwordCanBeReset.map(ue -> matches.size() > 1).orElse(Boolean.FALSE);
            optionalUserEmail = Optional.of(matches.get(0));
        } else {
            multipleMatchesAndCanBeReset = false;
            optionalUserEmail = userEmailRepository.findById(usernameOrEmailAddress.toUpperCase());
        }

        if (optionalUserEmail.isEmpty() || StringUtils.isEmpty(optionalUserEmail.get().getEmail())) {
            // no user found or email address found, so nothing more we can do.  Bail
            return Optional.empty();
        }

        final var userEmail = optionalUserEmail.get();
        final var templateAndParameters = getTemplateAndParameters(url, multipleMatchesAndCanBeReset, userEmail);

        sendEmail(userEmail.getUsername(), templateAndParameters, userEmail.getEmail());

        return Optional.ofNullable(templateAndParameters.getResetLink());
    }

    private TemplateAndParameters getTemplateAndParameters(final String url, final boolean multipleMatchesAndCanBeReset, final UserEmail userEmail) {
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
        if (multipleMatchesAndCanBeReset || passwordAllowedToBeReset(userEmail, userDetails)) {
            final var userTokenOptional = userTokenRepository.findByTokenTypeAndUserEmail(TokenType.RESET, userEmail);
            // delete any existing token
            userTokenOptional.ifPresent(userTokenRepository::delete);

            final var userToken = new UserToken(TokenType.RESET, userEmail);
            userTokenRepository.save(userToken);

            final var selectOrConfirm = multipleMatchesAndCanBeReset ? "select" : "confirm";
            final var resetLink = String.format("%s-%s?token=%s", url, selectOrConfirm, userToken.getToken());
            return new TemplateAndParameters(resetTemplateId, Map.of("firstName", firstName, "resetLink", resetLink));
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

        if (!passwordAllowedToBeReset(userEmail)) {
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
        sendPasswordResetEmail(userEmail);
    }

    private void sendPasswordResetEmail(final UserEmail userEmail) {
        // then the reset token
        final var username = userEmail.getUsername();
        final var email = userEmail.getEmail();
        final var parameters = Map.of("firstName", userEmail.getFirstName(), "username", username);

        // send the email
        try {
            log.info("Sending password reset to notify for user {}", username);
            notificationClient.sendEmail(resetPasswordConfirmedTemplateId, email, parameters, null);
        } catch (final NotificationClientException e) {
            final var reason = (e.getCause() != null ? e.getCause() : e).getClass().getSimpleName();
            log.warn("Failed to send password reset notify for user {}", username, e);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                try {
                    notificationClient.sendEmail(resetPasswordConfirmedTemplateId, email, parameters, null, null);
                } catch (NotificationClientException ex) {
                    log.error("Failed to send password reset notify for user {}", username, ex);
                }
            }
        }
    }

    private boolean passwordAllowedToBeReset(final UserEmail ue) {
        final var userPersonDetailsOptional = ue.isMaster() ? Optional.of(ue) : userService.findUser(ue.getUsername());
        return userPersonDetailsOptional.map(upd -> passwordAllowedToBeReset(ue, upd)).orElse(false);
    }

    @Override
    public String moveTokenToAccount(final String token, final String usernameInput) throws ResetPasswordException {
        if (StringUtils.isBlank(usernameInput)) {
            throw new ResetPasswordException("missing");
        }
        final var username = StringUtils.upperCase(StringUtils.trim(usernameInput));

        final var userEmailOptional = userEmailRepository.findById(username);
        return userEmailOptional.map(ue -> {
            final var userToken = userTokenRepository.findById(token).orElseThrow();
            // need to have same email address
            if (!userToken.getUserEmail().getEmail().equals(ue.getEmail())) {
                throw new ResetPasswordException("email");
            }

            if (!passwordAllowedToBeReset(ue)) {
                throw new ResetPasswordException("locked");
            }
            if (userToken.getUserEmail().getUsername().equals(username)) {
                // no work since they are the same
                return token;
            }
            // otherwise need to delete and add
            userTokenRepository.delete(userToken);
            final var newUserToken = new UserToken(TokenType.RESET, ue);
            userTokenRepository.save(newUserToken);
            return newUserToken.getToken();

        }).orElseThrow(() -> new ResetPasswordException("notfound"));
    }

    @AllArgsConstructor
    @Getter
    private static class TemplateAndParameters {
        private final String template;
        private final Map<String, String> parameters;

        TemplateAndParameters(final String template, final String firstName) {
            this.template = template;
            this.parameters = Map.of("firstName", firstName);
        }

        private String getResetLink() {
            return parameters.get("resetLink");
        }
    }

    // Throttling doesn't allow checked exceptions to be thrown, hence wrapped in a runtime exception
    public static class NotificationClientRuntimeException extends RuntimeException {

        public NotificationClientRuntimeException(final NotificationClientException e) {
            super(e);
        }
    }

    @Getter
    public static class ResetPasswordException extends RuntimeException {
        private final String reason;

        public ResetPasswordException(final String reason) {
            super(String.format("Reset Password failed with reason: %s", reason));
            this.reason = reason;
        }
    }
}

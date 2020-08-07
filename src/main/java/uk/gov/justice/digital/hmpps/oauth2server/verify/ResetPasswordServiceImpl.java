package uk.gov.justice.digital.hmpps.oauth2server.verify;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.service.DelegatingUserService;
import uk.gov.justice.digital.hmpps.oauth2server.utils.EmailHelper;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis;
import static uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.valueOf;

@Service
@Slf4j
@Transactional
public class ResetPasswordServiceImpl implements ResetPasswordService, PasswordService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final UserService userService;
    private final DelegatingUserService delegatingUserService;
    private final NotificationClientApi notificationClient;
    private final String resetTemplateId;
    private final String resetUnavailableTemplateId;
    private final String resetUnavailableEmailNotFoundTemplateId;
    private final String resetPasswordConfirmedTemplateId;

    public ResetPasswordServiceImpl(final UserRepository userRepository,
                                    final UserTokenRepository userTokenRepository,
                                    final UserService userService,
                                    final DelegatingUserService delegatingUserService,
                                    final NotificationClientApi notificationClient,
                                    @Value("${application.notify.reset.template}") final String resetTemplateId,
                                    @Value("${application.notify.reset-unavailable.template}") final String resetUnavailableTemplateId,
                                    @Value("${application.notify.reset-unavailable-email-not-found.template}") final String resetUnavailableEmailNotFoundTemplateId,
                                    @Value("${application.notify.reset-password.template}") final String resetPasswordConfirmedTemplateId) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.userService = userService;
        this.delegatingUserService = delegatingUserService;
        this.notificationClient = notificationClient;
        this.resetTemplateId = resetTemplateId;
        this.resetUnavailableTemplateId = resetUnavailableTemplateId;
        this.resetUnavailableEmailNotFoundTemplateId = resetUnavailableEmailNotFoundTemplateId;
        this.resetPasswordConfirmedTemplateId = resetPasswordConfirmedTemplateId;
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager")
    public Optional<String> requestResetPassword(final String usernameOrEmailAddress, final String url) throws NotificationClientRuntimeException {
        final Optional<User> optionalUser;
        final boolean multipleMatchesAndCanBeReset;
        if (StringUtils.contains(usernameOrEmailAddress, "@")) {
            final var email = EmailHelper.format(usernameOrEmailAddress);
            final var matches = userRepository.findByEmail(email);
            if (matches.isEmpty()) {
                // no match, but got an email address so let them know
                sendEmail(email, resetUnavailableEmailNotFoundTemplateId, Collections.emptyMap(), email);
                return Optional.empty();
            }

            final var passwordCanBeReset = matches.stream().filter(this::passwordAllowedToBeReset).findFirst();
            multipleMatchesAndCanBeReset = passwordCanBeReset.map(ue -> matches.size() > 1).orElse(Boolean.FALSE);
            optionalUser = Optional.of(matches.get(0));
        } else {
            multipleMatchesAndCanBeReset = false;

            optionalUser = userRepository.findByUsername(usernameOrEmailAddress.toUpperCase())
                    .or(() -> userService.findMasterUserPersonDetails(usernameOrEmailAddress.toUpperCase()).flatMap(userPersonDetails -> {

                        switch (valueOf(userPersonDetails.getAuthSource())) {
                            case nomis:
                                return saveNomisUser(userPersonDetails);
                            case delius:
                                return saveDeliusUser(userPersonDetails);
                        }
                        return Optional.empty();
                    }));
        }

        if (optionalUser.isEmpty() || StringUtils.isEmpty(optionalUser.get().getEmail())) {
            // no user found or email address found, so nothing more we can do.  Bail
            return Optional.empty();
        }

        final var user = optionalUser.get();
        final var templateAndParameters = getTemplateAndParameters(url, multipleMatchesAndCanBeReset, user);

        sendEmail(user.getUsername(), templateAndParameters, user.getEmail());

        return Optional.ofNullable(templateAndParameters.getResetLink());
    }

    private Optional<User> saveDeliusUser(final UserPersonDetails userPersonDetails) {
        final var user = userPersonDetails.toUser();
        if (!passwordAllowedToBeReset(user, userPersonDetails)) {
            return Optional.empty();
        }
        userRepository.save(user);
        return Optional.of(user);
    }

    private Optional<User> saveNomisUser(final UserPersonDetails userPersonDetails) {
        final var user = userPersonDetails.toUser();
        if (!passwordAllowedToBeReset(user, userPersonDetails)) {
            return Optional.empty();
        }
        return userService.getEmailAddressFromNomis(user.getUsername()).map(e -> {
            user.setEmail(e);
            userRepository.save(user);
            return user;
        });
    }

    private TemplateAndParameters getTemplateAndParameters(final String url, final boolean multipleMatchesAndCanBeReset, final User user) {
        final UserPersonDetails userDetails;
        if (user.isMaster()) {
            userDetails = user;
        } else {
            final var userOptional = userService.findMasterUserPersonDetails(user.getUsername());
            if (userOptional.isEmpty()) {
                // shouldn't really happen, means that a nomis user exists in auth but not in nomis
                return new TemplateAndParameters(resetUnavailableTemplateId, user.getUsername(), user.getName());
            }
            userDetails = userOptional.get();
        }
        // only allow reset for active accounts that aren't locked
        // or are locked by getting password incorrect (in either c-nomis or auth)
        final var firstName = userDetails.getFirstName();
        final var fullName = userDetails.getName();
        if (multipleMatchesAndCanBeReset || passwordAllowedToBeReset(user, userDetails)) {
            final var userToken = user.createToken(TokenType.RESET);

            final var selectOrConfirm = multipleMatchesAndCanBeReset ? "select" : "confirm";
            final var resetLink = String.format("%s-%s?token=%s", url, selectOrConfirm, userToken.getToken());
            return new TemplateAndParameters(resetTemplateId, Map.of("firstName", firstName, "fullName", fullName, "resetLink", resetLink));
        }
        return new TemplateAndParameters(resetUnavailableTemplateId, firstName, fullName);
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

    private boolean passwordAllowedToBeReset(final User user, final UserPersonDetails userPersonDetails) {
        if (user.getSource() != nomis) {
            // for non nomis users they must be enabled (so can be locked)
            return userPersonDetails.isEnabled();
        }
        // otherwise must be nomis user so will have a staff account instead.
        final var staffUserAccount = (NomisUserPersonDetails) userPersonDetails;
        final var status = staffUserAccount.getAccountDetail().getStatus();
        return staffUserAccount.getStaff().isActive() && (!status.isLocked() || status.isUserLocked() || user.isLocked());
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager")
    public void setPassword(final String token, final String password) {
        final var userToken = userTokenRepository.findById(token).orElseThrow();
        final var user = userToken.getUser();

        final var userPersonDetails = user.isMaster() ? user : userService.findMasterUserPersonDetails(user.getUsername()).orElseThrow();
        if (!passwordAllowedToBeReset(user, userPersonDetails)) {
            // failed, so let user know
            throw new LockedException("locked");
        }

        delegatingUserService.changePasswordWithUnlock(userPersonDetails, password);

        user.removeToken(userToken);
        userRepository.save(user);
        sendPasswordResetEmail(user);
    }

    private void sendPasswordResetEmail(final User user) {
        // then the reset token
        final var username = user.getUsername();
        final var email = user.getEmail();
        final var parameters = Map.of("firstName", user.getFirstName(), "fullName", user.getName(), "username", username);

        // send the email
        try {
            log.info("Sending password reset to notify for user {}", username);
            notificationClient.sendEmail(resetPasswordConfirmedTemplateId, email, parameters, null);
        } catch (final NotificationClientException e) {
            final var reason = (e.getCause() != null ? e.getCause() : e).getClass().getSimpleName();
            log.warn("Failed to send password reset notify for user {} due to {}", username, reason, e);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                try {
                    notificationClient.sendEmail(resetPasswordConfirmedTemplateId, email, parameters, null, null);
                } catch (final NotificationClientException ex) {
                    log.error("Failed to send password reset notify for user {}", username, ex);
                }
            }
        }
    }

    private boolean passwordAllowedToBeReset(final User ue) {
        final var userPersonDetailsOptional = ue.isMaster() ? Optional.of(ue) : userService.findMasterUserPersonDetails(ue.getUsername());
        return userPersonDetailsOptional.map(upd -> passwordAllowedToBeReset(ue, upd)).orElse(false);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager")
    public String moveTokenToAccount(final String token, final String usernameInput) throws ResetPasswordException {
        if (StringUtils.isBlank(usernameInput)) {
            throw new ResetPasswordException("missing");
        }
        final var username = StringUtils.upperCase(StringUtils.trim(usernameInput));

        final var userOptional = userRepository.findByUsername(username);
        return userOptional.map(ue -> {
            final var userToken = userTokenRepository.findById(token).orElseThrow();
            // need to have same email address
            if (!userToken.getUser().getEmail().equals(ue.getEmail())) {
                throw new ResetPasswordException("email");
            }

            if (!passwordAllowedToBeReset(ue)) {
                throw new ResetPasswordException("locked");
            }
            if (userToken.getUser().getUsername().equals(username)) {
                // no work since they are the same
                return token;
            }
            // otherwise need to delete and add
            userTokenRepository.delete(userToken);
            final var newUserToken = ue.createToken(TokenType.RESET);
            return newUserToken.getToken();

        }).orElseThrow(() -> new ResetPasswordException("notfound"));
    }

    @AllArgsConstructor
    @Getter
    private static class TemplateAndParameters {
        private final String template;
        private final Map<String, String> parameters;

        TemplateAndParameters(final String template, final String firstName, final String fullName) {
            this.template = template;
            this.parameters = Map.of("firstName", firstName, "fullName", fullName);
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

    public static class ResetPasswordException extends RuntimeException {
        private final String reason;

        public ResetPasswordException(final String reason) {
            super(String.format("Reset Password failed with reason: %s", reason));
            this.reason = reason;
        }

        public String getReason() {
            return this.reason;
        }
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.verify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class ResetPasswordService implements PasswordService {

    private final UserEmailRepository userEmailRepository;
    private final UserTokenRepository userTokenRepository;
    private final UserService userService;
    private final AlterUserService alterUserService;
    private final NotificationClientApi notificationClient;
    private final String resetTemplateId;
    private final String resetUnavailableTemplateId;
    private final PasswordEncoder passwordEncoder;
    private final long passwordAge;

    public ResetPasswordService(final UserEmailRepository userEmailRepository,
                                final UserTokenRepository userTokenRepository, final UserService userService,
                                final AlterUserService alterUserService,
                                final NotificationClientApi notificationClient,
                                @Value("${application.notify.reset.template}") final String resetTemplateId,
                                @Value("${application.notify.reset-unavailable.template}") final String resetUnavailableTemplateId,
                                final PasswordEncoder passwordEncoder,
                                @Value("${application.authentication.password-age}") final long passwordAge) {
        this.userEmailRepository = userEmailRepository;
        this.userTokenRepository = userTokenRepository;
        this.userService = userService;
        this.alterUserService = alterUserService;
        this.notificationClient = notificationClient;
        this.resetTemplateId = resetTemplateId;
        this.resetUnavailableTemplateId = resetUnavailableTemplateId;
        this.passwordEncoder = passwordEncoder;
        this.passwordAge = passwordAge;
    }

    public Optional<String> requestResetPassword(final String inputUsername, final String url) throws NotificationClientException {
        final var username = inputUsername.toUpperCase();
        final var optionalUserEmail = userEmailRepository.findById(username);

        if (optionalUserEmail.isEmpty() || StringUtils.isEmpty(optionalUserEmail.get().getEmail())) {
            // no user found or email address found, so nothing more we can do.  Bail
            return Optional.empty();
        }

        final var userEmail = optionalUserEmail.get();
        final var userOptional = userEmail.isMaster() ? optionalUserEmail : userService.findUser(username);

        final String emailTemplate;
        final Map<String, String> parameters;
        if (userOptional.isEmpty()) {
            emailTemplate = resetUnavailableTemplateId;
            parameters = Map.of("firstName", username);
        } else {
            final var userDetails = userOptional.get();
            // only allow reset for active accounts that aren't locked
            // or are locked by getting password incorrect (in either c-nomis or auth)
            final var firstName = userDetails.getFirstName();
            if (passwordAllowedToBeReset(userEmail, userDetails)) {
                emailTemplate = resetTemplateId;
                final var userTokenOptional = userTokenRepository.findByTokenTypeAndUserEmail(TokenType.RESET, userEmail);
                // delete any existing token
                userTokenOptional.ifPresent(userTokenRepository::delete);

                final var userToken = new UserToken(TokenType.RESET, userEmail);
                userTokenRepository.save(userToken);

                final var resetLink = url + userToken.getToken();
                parameters = Map.of("firstName", firstName, "resetLink", resetLink);
            } else {
                emailTemplate = resetUnavailableTemplateId;
                parameters = Map.of("firstName", firstName);
            }
        }

        final var email = userEmail.getEmail();
        try {
            log.info("Sending reset password to notify for user {}", username);
            notificationClient.sendEmail(emailTemplate, email, parameters, null);
        } catch (final NotificationClientException e) {
            log.warn("Failed to send reset password to notify for user {}", username, e);
            if (e.getHttpResult() >= 500) {
                // second time lucky
                notificationClient.sendEmail(emailTemplate, email, parameters, null, null);
            }
            throw e;
        }

        return Optional.ofNullable(parameters.get("resetLink"));
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
        final var userOptional = userService.findUser(userEmail.getUsername());

        // before we reset, ensure user allowed to still reset password
        if (userOptional.isEmpty() || !passwordAllowedToBeReset(userEmail, userOptional.get())) {
            // failed, so let user know
            throw new LockedException("locked");
        }

        userEmail.setLocked(false);

        // if we're the master of this user record deal with the change of password here
        if (userEmail.isMaster()) {
            userEmail.setPassword(passwordEncoder.encode(password));
            userEmail.setPasswordExpiry(LocalDateTime.now().plusDays(passwordAge));
        } else {
            alterUserService.changePasswordWithUnlock(userEmail.getUsername(), password);
        }

        userEmailRepository.save(userEmail);
        userTokenRepository.delete(userToken);
    }
}

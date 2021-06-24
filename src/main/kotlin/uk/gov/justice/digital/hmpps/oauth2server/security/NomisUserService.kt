package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User.EmailType.PRIMARY
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService.LinkEmailAndUsername
import java.util.Optional

@Service
@Transactional(readOnly = true)
abstract class NomisUserService(
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val userRepository: UserRepository,
  private val verifyEmailService: VerifyEmailService,
) {
  fun getNomisUserByUsername(username: String): Optional<NomisUserPersonDetails> =
    staffUserAccountRepository.findById(username.uppercase())

  fun getNomisUsersByEmail(email: String): List<NomisUserPersonDetails> {
    val emailLowered = email.lowercase()

    val allNomisInAuthUsernames = userRepository.findByEmailAndSourceOrderByUsername(emailLowered, nomis)
      .filter { it.verified }
      .map { it.username }

    val allNomisInAuth = if (allNomisInAuthUsernames.size > 0)
      staffUserAccountRepository.findAllById(allNomisInAuthUsernames).toSet() else setOf()

    val allNomis = staffUserAccountRepository.findAllNomisUsersByEmailAddress(emailLowered)
      .toSet()

    return allNomis.union(allNomisInAuth).toList()
  }

  fun findPrisonUsersByFirstAndLastNames(firstName: String, lastName: String): List<NomisUserPersonDetails> {
    return staffUserAccountRepository.findByStaffFirstNameIgnoreCaseAndStaffLastNameIgnoreCase(firstName, lastName)
  }

  fun changeEmailAndRequestVerification(username: String, emailInput: String?, url: String, emailType: EmailType): LinkEmailAndUsername {
    val user = getNomisUserByUsername(username)
      .orElseThrow { UsernameNotFoundException("Account for username $username not found") }

    return verifyEmailService.changeEmailAndRequestVerification(
      username = user.username,
      emailInput = emailInput,
      firstName = user.firstName,
      fullname = user.name,
      url = url,
      emailType = PRIMARY
    )
  }

  abstract fun changePassword(username: String?, password: String?)
  abstract fun changePasswordWithUnlock(username: String?, password: String?)
  abstract fun lockAccount(username: String?)
}

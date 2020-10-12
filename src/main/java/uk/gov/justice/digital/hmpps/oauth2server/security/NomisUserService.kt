package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import java.util.Optional

@Service
@Transactional(readOnly = true)
abstract class NomisUserService(
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val userRepository: UserRepository,
) {
  fun getNomisUserByUsername(username: String): Optional<NomisUserPersonDetails> =
    staffUserAccountRepository.findById(username.toUpperCase())

  fun getNomisUsersByEmail(email: String): List<NomisUserPersonDetails> {
    val emailLowered = email.toLowerCase()

    val allNomisInAuthUsernames = userRepository.findByEmailAndSourceOrderByUsername(emailLowered, nomis)
      .filter { it.isVerified }
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

  abstract fun changePassword(username: String?, password: String?)
  abstract fun changePasswordWithUnlock(username: String?, password: String?)
  abstract fun lockAccount(username: String?)
}

package uk.gov.justice.digital.hmpps.oauth2server.security

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository
import java.util.*

@Service
@Transactional(readOnly = true)
abstract class NomisUserService(private val staffUserAccountRepository: StaffUserAccountRepository) {
  fun getNomisUserByUsername(username: String): Optional<NomisUserPersonDetails> =
      staffUserAccountRepository.findById(StringUtils.upperCase(username))

  abstract fun changePassword(username: String?, password: String?)
  abstract fun changePasswordWithUnlock(username: String?, password: String?)
  abstract fun lockAccount(username: String?)
}

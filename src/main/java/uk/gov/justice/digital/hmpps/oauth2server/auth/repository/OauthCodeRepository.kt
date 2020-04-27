package uk.gov.justice.digital.hmpps.oauth2server.auth.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.OauthCode
import java.time.LocalDateTime


interface OauthCodeRepository : CrudRepository<OauthCode, String> {
  fun deleteByCreatedDateBefore(createDate: LocalDateTime)
}


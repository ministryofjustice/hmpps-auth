package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "DBA_USERS")
data class AccountDetail(
  @Id
  @Column(name = "USERNAME", nullable = false)
  val username: String,

  @Column(name = "ACCOUNT_STATUS", nullable = false)
  val accountStatus: String = AccountStatus.EXPIRED_LOCKED.desc,

  @Column(name = "PROFILE")
  val profile: String = AccountProfile.TAG_GENERAL.name,

  @Column(name = "expiry_date")
  val passwordExpiry: LocalDateTime? = null
) {

  val status: AccountStatus
    get() = AccountStatus.get(accountStatus)
  val accountProfile: AccountProfile
    get() = AccountProfile.valueOf(profile.toUpperCase())
}

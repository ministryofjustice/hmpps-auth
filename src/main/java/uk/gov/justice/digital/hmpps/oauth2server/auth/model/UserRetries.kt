package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "USER_RETRIES")
data class UserRetries(
    @Id
    @Column(name = "username", nullable = false)
    val username: String? = null) {

  @Column(name = "retry_count")
  var retryCount: Int = 0

  constructor(username: String, retryCount: Int) : this(username) {
    this.retryCount = retryCount
  }

  fun incrementRetryCount() {
    retryCount++
  }
}

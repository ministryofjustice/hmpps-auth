package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.Date
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

@Entity
@Table(name = "REFERENCE_CODES")
data class ReferenceCode(
  @EmbeddedId
  val domainCodeIdentifier: DomainCodeIdentifier,

  @Column(name = "DESCRIPTION", nullable = false)
  val description: String,

  @Type(type = "yes_no")
  @Column(name = "ACTIVE_FLAG", nullable = false)
  val active: Boolean = false,

  @Column(name = "EXPIRED_DATE")
  val expiredDate: Date? = null,
)

@Embeddable
data class DomainCodeIdentifier(
  @Column(name = "DOMAIN", nullable = false)
  @Enumerated(EnumType.STRING)
  private val domain: ReferenceDomain,

  @Column(name = "CODE", nullable = false)
  private val code: String,
) : Serializable

enum class ReferenceDomain(val domain: String) {
  EMAIL_DOMAIN("EMAIL_DOMAIN")
}

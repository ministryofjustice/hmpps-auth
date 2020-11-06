package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "OMS_ROLES")
data class Role(
  @Id
  @Column(name = "ROLE_ID", nullable = false)
  val id: Long,

  @Column(name = "ROLE_CODE", nullable = false, unique = true)
  val code: String,

  @Column(name = "ROLE_NAME")
  val name: String? = null,
)

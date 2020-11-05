package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import java.io.Serializable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "USER_CASELOAD_ROLES")
data class UserCaseloadRole(
  @EmbeddedId
  val id: UserCaseloadRoleIdentity,

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "ROLE_ID", updatable = false, insertable = false)
  val role: Role,
) : Serializable

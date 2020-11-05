package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import uk.gov.justice.digital.hmpps.oauth2server.auth.model.GroupAssignableRole.GroupAssignableRoleId
import java.io.Serializable
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "GROUP_ASSIGNABLE_ROLE")
@IdClass(GroupAssignableRoleId::class)
data class GroupAssignableRole(

  @Id
  @ManyToOne
  @JoinColumn(name = "role_id")
  val role: Authority? = null,

  @Id
  @ManyToOne
  @JoinColumn(name = "group_id")
  val group: Group? = null,

  val automatic: Boolean = false,
) : Serializable {

  class GroupAssignableRoleId : Serializable {
    private val group: UUID? = null
    private val role: UUID? = null
  }

  override fun toString(): String {
    return "GroupAssignableRole(role=$role, automatic=$automatic)"
  }
}

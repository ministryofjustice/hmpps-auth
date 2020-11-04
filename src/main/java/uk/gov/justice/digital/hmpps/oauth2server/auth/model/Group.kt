package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.hibernate.annotations.GenericGenerator
import java.io.Serializable
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "GROUPS")
class Group(
  @Column(name = "group_code", nullable = false) val groupCode: String,
  @Column(name = "group_name", nullable = false) val groupName: String,
) : Serializable {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "group_id", updatable = false, nullable = false)
  val id: UUID? = null

  @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true)
  val assignableRoles: MutableSet<GroupAssignableRole> = mutableSetOf()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Group

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()
}

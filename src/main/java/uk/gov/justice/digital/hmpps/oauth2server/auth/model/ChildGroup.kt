package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.hibernate.annotations.GenericGenerator
import java.io.Serializable
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "CHILD_GROUP")
class ChildGroup(
  @Column(name = "child_group_code", nullable = false) val groupCode: String,
  @Column(name = "child_group_name", nullable = false) val groupName: String,
) : Serializable {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "child_group_id", updatable = false, nullable = false)
  val id: UUID? = null

  @Id
  @ManyToOne
  @JoinColumn(name = "group_id")
  val group: Group? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ChildGroup) return false

    if (groupCode != other.groupCode) return false

    return true
  }

  override fun hashCode(): Int = groupCode.hashCode()
}

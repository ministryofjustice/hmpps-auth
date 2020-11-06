package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.GenericGenerator
import org.springframework.security.core.GrantedAuthority
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "ROLES")
class Authority(roleCode: String, roleName: String) : GrantedAuthority {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "role_id", updatable = false, nullable = false)
  val id: UUID? = null

  @Column(name = "role_code", nullable = false)
  val roleCode: String

  @Column(name = "role_name", nullable = false)
  val roleName: String
  override fun getAuthority(): String = "$ROLE_PREFIX$roleCode"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Authority

    if (roleCode != other.roleCode) return false

    return true
  }

  override fun hashCode(): Int = roleCode.hashCode()

  companion object {
    const val ROLE_PREFIX = "ROLE_"
    fun removeRolePrefixIfNecessary(role: String): String =
      if (StringUtils.startsWith(role, ROLE_PREFIX)) StringUtils.substring(role, ROLE_PREFIX.length) else role
  }

  init {
    this.roleCode = removeRolePrefixIfNecessary(roleCode)
    this.roleName = roleName
  }
}

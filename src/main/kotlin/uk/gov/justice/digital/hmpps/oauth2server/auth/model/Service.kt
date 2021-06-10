package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.apache.commons.lang3.StringUtils.trimToNull
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "OAUTH_SERVICE")
data class Service(
  @Id
  @Column(nullable = false)
  val code: String,
  @Column(nullable = false)
  val name: String,

  @Column(nullable = false)
  val description: String,

  @Column(name = "authorised_roles")
  private var authorisedRoles: String? = null,

  @Column(nullable = false)
  val url: String,

  @Column(nullable = false)
  val enabled: Boolean = false,

  @Column
  val email: String? = null,
) {

  val roles: List<String>
    get() = authorisedRoles?.split(',')?.mapNotNull { trimToNull(it) } ?: emptyList()

  val isUrlInsteadOfEmail: Boolean
    get() = email?.startsWith("http") ?: false

  var authorisedRolesWithNewlines: String
    get() = authorisedRoles?.replace(",".toRegex(), "\n") ?: ""
    set(authorisedRolesWithNewlines) {
      authorisedRoles = authorisedRolesWithNewlines
        .replace("\n".toRegex(), ",")
        .split(',')
        .mapNotNull { trimToNull(it) }
        .map { it.uppercase() }
        .map { if (it.startsWith("ROLE_")) it else "ROLE_$it" }
        .joinToString(",")
    }
}

package uk.gov.justice.digital.hmpps.oauth2server.auth.model

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
  var authorisedRoles: String? = null,

  @Column(nullable = false)
  val url: String,

  @Column(nullable = false)
  val enabled: Boolean = false,

  @Column
  val email: String? = null,
) {

  val roles: List<String>
    get() = authorisedRoles?.split(',')?.map { it.trim() } ?: emptyList()

  val isUrlInsteadOfEmail: Boolean
    get() = email?.startsWith("http") ?: false

  var authorisedRolesWithNewlines: String
    get() = authorisedRoles?.replace(",".toRegex(), "\n") ?: ""
    set(authorisedRolesWithNewlines) {
      authorisedRoles = authorisedRolesWithNewlines.replace("\n".toRegex(), ",")
    }
}

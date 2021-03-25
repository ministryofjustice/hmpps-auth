@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.provider.ClientDetails
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Suppress("JpaDataSourceORMInspection")
@Entity
@Table(name = "oauth_client_details")
data class Client(
  @Id
  @Column(name = "client_id", nullable = false)
  val id: String
) : ClientDetails {
  override fun getClientId(): String = id

  override fun getResourceIds(): MutableSet<String> {
    TODO("Not yet implemented")
  }

  override fun isSecretRequired(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getClientSecret(): String {
    TODO("Not yet implemented")
  }

  override fun isScoped(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getScope(): MutableSet<String> {
    TODO("Not yet implemented")
  }

  override fun getAuthorizedGrantTypes(): MutableSet<String> {
    TODO("Not yet implemented")
  }

  override fun getRegisteredRedirectUri(): MutableSet<String> {
    TODO("Not yet implemented")
  }

  override fun getAuthorities(): MutableCollection<GrantedAuthority> {
    TODO("Not yet implemented")
  }

  override fun getAccessTokenValiditySeconds(): Int {
    TODO("Not yet implemented")
  }
  override fun getRefreshTokenValiditySeconds(): Int {
    TODO("Not yet implemented")
  }

  override fun isAutoApprove(scope: String?): Boolean {
    TODO("Not yet implemented")
  }

  override fun getAdditionalInformation(): MutableMap<String, Any> {
    TODO("Not yet implemented")
  }
}

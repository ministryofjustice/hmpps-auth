@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.cache.annotation.Cacheable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import javax.sql.DataSource

open class CachingClientDetailsService(dataSource: DataSource, passwordEncoder: PasswordEncoder) :
  JdbcClientDetailsService(dataSource) {

  init {
    super.setPasswordEncoder(passwordEncoder)
  }

  @Cacheable(value = ["loadClientByClientId"])
  override fun loadClientByClientId(clientId: String?): ClientDetails = super.loadClientByClientId(clientId)
}

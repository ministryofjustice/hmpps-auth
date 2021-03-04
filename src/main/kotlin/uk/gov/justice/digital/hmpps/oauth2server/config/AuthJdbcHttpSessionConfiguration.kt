package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Configuration
import org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class AuthJdbcHttpSessionConfiguration(
  @Qualifier("authTransactionManager") transactionManager: PlatformTransactionManager,
) : JdbcHttpSessionConfiguration() {
  init {
    @Suppress("LeakingThis")
    setTransactionManager(transactionManager)
  }
}

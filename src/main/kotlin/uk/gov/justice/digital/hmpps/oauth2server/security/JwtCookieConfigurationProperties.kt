package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "jwt.cookie")
data class JwtCookieConfigurationProperties(
  val name: String,
  val expiryTime: Duration,
)

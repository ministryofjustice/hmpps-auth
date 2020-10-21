package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "saved-request.cookie")
data class SavedRequestCookieConfigurationProperties(val name: String, val expiryTime: Duration)

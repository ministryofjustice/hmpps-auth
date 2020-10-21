package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientApi

@Configuration
class NotificationConfig {
  @Bean
  fun notificationClient(@Value("\${application.notify.key:invalidkey}") key: String): NotificationClientApi =
    NotificationClient(key)
}

package uk.gov.justice.digital.hmpps.oauth2server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
class Oauth2serverApplication

fun main(args: Array<String>) {
  runApplication<Oauth2serverApplication>(*args)
}

@Configuration
@EnableScheduling
class SchedulingConfiguration

@Configuration
@EnableCaching
class CachingConfiguration

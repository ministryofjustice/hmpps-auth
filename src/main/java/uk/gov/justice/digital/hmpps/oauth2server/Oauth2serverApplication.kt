package uk.gov.justice.digital.hmpps.oauth2server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class Oauth2serverApplication

fun main(args: Array<String>) {
  runApplication<Oauth2serverApplication>(*args)
}

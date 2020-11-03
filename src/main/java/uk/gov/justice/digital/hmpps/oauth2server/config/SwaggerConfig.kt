package uk.gov.justice.digital.hmpps.oauth2server.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Predicates
import io.swagger.util.ReferenceSerializationConfigurer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.json.JacksonModuleRegistrar
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Date
import java.util.Optional

@Configuration
@EnableSwagger2
class SwaggerConfig {

  @Bean
  fun offenderApi(buildProperties: BuildProperties): Docket {
    val docket = Docket(DocumentationType.SWAGGER_2)
      .useDefaultResponseMessages(false)
      .apiInfo(apiInfo(buildProperties))
      .select()
      .apis(RequestHandlerSelectors.any())
      .paths(
        Predicates.or(
          PathSelectors.regex("(\\/info.*)"),
          PathSelectors.regex("(\\/api.*)"),
          PathSelectors.regex("(\\/health)")
        )
      )
      .build()
    docket.genericModelSubstitutes(Optional::class.java)
    docket.directModelSubstitute(ZonedDateTime::class.java, Date::class.java)
    docket.directModelSubstitute(LocalDateTime::class.java, Date::class.java)
    return docket
  }

  private fun apiInfo(buildProperties: BuildProperties): ApiInfo =
    ApiInfo(
      "HMPPS Auth",
      "HMPPS Auth API Documentation",
      buildProperties.version,
      "",
      contactInfo(),
      "Open Government Licence v3.0",
      "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
      emptyList()
    )

  private fun contactInfo() = Contact("HMPPS Digital Studio", "", "feedback@digital.justice.gov.uk")

  @Bean
  fun swaggerJacksonModuleRegistrar() = JacksonModuleRegistrar { mapper: ObjectMapper ->
    ReferenceSerializationConfigurer.serializeAsComputedRef(mapper)
  }
}

package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.method.annotation.PrincipalMethodArgumentResolver

/**
 * Workaround for this issue: https://github.com/spring-projects/spring-framework/issues/26117
 */
@Configuration
class WebConfig : WebMvcConfigurer {
  override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
    super.addArgumentResolvers(resolvers)

    resolvers.add(PrincipalMethodArgumentResolver())
  }
}

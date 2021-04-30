@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@Order(1)
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@EnableResourceServer
class ResourceConfiguration(
  private val tokenServices: ResourceServerTokenServices,
  private val authExpressionHandler: OAuth2WebSecurityExpressionHandler,
) : ResourceServerConfigurerAdapter() {
  override fun configure(http: HttpSecurity) {
    http {
      securityMatcher(AntPathRequestMatcher("/api/**"))
      authorizeRequests {
        authorize(pattern = "/api/client/**", access = "isAuthenticated() and @authIpSecurity.check(request)")
        authorize(pattern = "/api/**")
      }
    }
  }

  override fun configure(config: ResourceServerSecurityConfigurer) {
    config.expressionHandler(authExpressionHandler)
    config.tokenServices(tokenServices)
  }
}
